package com.microchip.lambda_core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Comparator;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShareStorage {

    private static final int BUFFER_SIZE = 8192;

    private final Path root;
    private final long maxFileBytes;

    public ShareStorage(StorageProperties properties) {
        this.root = Path.of(properties.root()).resolve("share");
        this.maxFileBytes = properties.maxFileBytes();
    }

    public long write(UUID shareId, UUID fileId, InputStream body) throws IOException {
        Path target = locate(shareId, fileId);
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(
                target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            long total = 0;
            int read;
            while ((read = body.read(buffer)) != -1) {
                total += read;
                // checked before writing if file over limit
                if (total > maxFileBytes) {
                    throw new FileTooLargeException(maxFileBytes);
                }
                out.write(buffer, 0, read);
            }
            return total;
        } catch (IOException | RuntimeException e) {
            Files.deleteIfExists(target);
            throw e;
        }
    }

    public Path locate(UUID shareId, UUID fileId) {
        return root.resolve(shareId.toString()).resolve(fileId.toString());
    }

    public void deleteShareDir(UUID shareId) throws IOException {
        Path dir = root.resolve(shareId.toString());
        if (!Files.exists(dir)) {
            return;
        }
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
