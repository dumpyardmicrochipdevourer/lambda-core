package com.microchip.lambda_core.storage;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.ShareFile;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.springframework.stereotype.Component;

@Component
public class ShareStorage {

    private static final int BUFFER_SIZE = 8192;

    // the free-space floor is rechecked this often while a body streams in
    private static final long DISK_CHECK_EVERY = 4L * 1024 * 1024;

    private final Path root;
    private final long maxFileBytes;
    private final StorageBudget budget;

    private final Counter uploaded;

    public ShareStorage(StorageProperties properties, StorageBudget budget, MeterRegistry registry) {
        this.root = Path.of(properties.root()).resolve("share");
        this.maxFileBytes = properties.maxFileBytes();
        this.budget = budget;
        this.uploaded = Counter.builder("lambda.uploaded").baseUnit("bytes").tag("kind", "share").register(registry);
    }

    public long write(UUID shareId, UUID fileId, InputStream body) throws IOException {
        Path target = locate(shareId, fileId);
        Files.createDirectories(target.getParent());
        long total = 0;
        try (OutputStream out = Files.newOutputStream(
                target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = body.read(buffer)) != -1) {
                // checked before writing if file over limit
                if (total + read > maxFileBytes) {
                    throw new FileTooLargeException(maxFileBytes);
                }
                budget.takeShare(read);
                total += read;
                if (total / DISK_CHECK_EVERY != (total - read) / DISK_CHECK_EVERY) {
                    budget.requireDiskRoom(0);
                }
                out.write(buffer, 0, read);
                uploaded.increment(read);
            }
            return total;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            budget.giveBackShare(total);
            throw e;
        }
    }

    public Path locate(UUID shareId, UUID fileId) {
        return root.resolve(shareId.toString()).resolve(fileId.toString());
    }

    public void writeArchive(UUID shareId, List<ShareFile> files, OutputStream out) throws IOException {
        try (ZipOutputStream zip = new ZipOutputStream(out)) {
            zip.setLevel(Deflater.NO_COMPRESSION);
            for (ShareFile file : files) {
                if (file.getStatus() != FileStatus.COMPLETE) {
                    continue;
                }
                zip.putNextEntry(new ZipEntry(file.getName()));
                Files.copy(locate(shareId, file.getId()), zip);
                zip.closeEntry();
            }
        }
    }

    public void deleteShareDir(UUID shareId) throws IOException {
        Path dir = root.resolve(shareId.toString());
        if (!Files.exists(dir)) {
            return;
        }
        budget.giveBackShare(StorageBudget.sizeOf(dir));
        // TwT
        try (var paths = Files.walk(dir)) {
            // Files.delete only works on empty dirs, so children must
            // go before their parent (reverse order gives exactly that).
            paths.sorted(Comparator.reverseOrder()).forEach(p -> {
                try {
                    Files.delete(p);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }
    }
}
