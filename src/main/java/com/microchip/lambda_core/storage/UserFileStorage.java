package com.microchip.lambda_core.storage;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class UserFileStorage {

    private static final int BUFFER_SIZE = 8192;

    private static final long DISK_CHECK_EVERY = 4L * 1024 * 1024;

    private final Path root;
    private final StorageBudget budget;

    private final Counter uploaded;

    public UserFileStorage(StorageProperties properties, StorageBudget budget, MeterRegistry registry) {
        this.root = Path.of(properties.root()).resolve("user");
        this.budget = budget;
        this.uploaded = Counter.builder("lambda.uploaded").baseUnit("bytes").tag("kind", "personal").register(registry);
    }

    public Path locate(UUID ownerId, UUID fileId) {
        return root.resolve(ownerId.toString()).resolve(fileId.toString());
    }

    public long sizeOnDisk(UUID ownerId, UUID fileId) throws IOException {
        Path path = locate(ownerId, fileId);
        return Files.exists(path) ? Files.size(path) : 0;
    }

    // whatever made it to disk stays there, so a dropped connection can be resumed from sizeOnDisk
    public void append(UUID ownerId, UUID fileId, InputStream body, long limit) throws IOException {
        Path target = locate(ownerId, fileId);
        Files.createDirectories(target.getParent());
        long have = Files.exists(target) ? Files.size(target) : 0;
        budget.requireDiskRoom(0);
        try (OutputStream out = Files.newOutputStream(target, StandardOpenOption.CREATE, StandardOpenOption.APPEND)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = body.read(buffer)) != -1) {
                if (have + read > limit) {
                    out.write(buffer, 0, (int) (limit - have));
                    throw new FileTooLargeException(limit);
                }
                out.write(buffer, 0, read);
                uploaded.increment(read);
                have += read;
                if (have / DISK_CHECK_EVERY != (have - read) / DISK_CHECK_EVERY) {
                    budget.requireDiskRoom(0);
                }
            }
        }
    }

    public void delete(UUID ownerId, UUID fileId) throws IOException {
        Files.deleteIfExists(locate(ownerId, fileId));
    }
}
