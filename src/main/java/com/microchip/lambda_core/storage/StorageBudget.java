package com.microchip.lambda_core.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicLong;
import org.springframework.stereotype.Component;

// Two limits no upload may cross: the swap as a whole has a ceiling, and the disk
// keeps a floor of free space so the database and the neighbours never run dry.
@Component
public class StorageBudget {

    private final Path root;
    private final long shareLimit;
    private final long minFree;
    private final AtomicLong shareUsed = new AtomicLong();

    public StorageBudget(StorageProperties properties) {
        this.root = Path.of(properties.root());
        this.shareLimit = properties.shareTotalBytes();
        this.minFree = properties.minFreeBytes();
        try {
            Files.createDirectories(root);
            shareUsed.set(sizeOf(root.resolve("share")));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public void takeShare(long bytes) {
        if (shareUsed.addAndGet(bytes) > shareLimit) {
            shareUsed.addAndGet(-bytes);
            throw new StorageFullException();
        }
    }

    public void giveBackShare(long bytes) {
        shareUsed.addAndGet(-bytes);
    }

    public void requireShareRoom(long bytes) {
        if (shareUsed.get() + bytes > shareLimit) {
            throw new StorageFullException();
        }
        requireDiskRoom(bytes);
    }

    public void requireDiskRoom(long bytes) {
        if (diskFree() - bytes < minFree) {
            throw new StorageFullException();
        }
    }

    public long shareUsed() { return shareUsed.get(); }
    public long shareLimit() { return shareLimit; }
    public long minFree() { return minFree; }

    public long diskFree() {
        try {
            return Files.getFileStore(root).getUsableSpace();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static long sizeOf(Path dir) throws IOException {
        if (!Files.exists(dir)) {
            return 0;
        }
        try (var paths = Files.walk(dir)) {
            return paths.filter(Files::isRegularFile).mapToLong(p -> p.toFile().length()).sum();
        }
    }
}
