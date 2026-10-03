package com.microchip.lambda_core.storage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ShareStorageTest {

    @TempDir Path root;

    @Test
    void swapAsAWholeHasACeilingAndGivesSpaceBack() throws IOException {
        StorageBudget budget = budget(1000, 0);
        ShareStorage storage = new ShareStorage(properties(1000, 0), budget, new SimpleMeterRegistry());
        UUID first = UUID.randomUUID();

        storage.write(first, UUID.randomUUID(), body(600));
        assertEquals(600, budget.shareUsed());

        UUID second = UUID.randomUUID();
        UUID file = UUID.randomUUID();
        assertThrows(StorageFullException.class, () -> storage.write(second, file, body(500)));
        assertEquals(600, budget.shareUsed());
        assertEquals(false, Files.exists(storage.locate(second, file)));

        storage.deleteShareDir(first);
        assertEquals(0, budget.shareUsed());
        storage.write(second, file, body(500));
    }

    @Test
    void usageIsCountedFromDiskOnStart() throws IOException {
        new ShareStorage(properties(1000, 0), budget(1000, 0), new SimpleMeterRegistry())
                .write(UUID.randomUUID(), UUID.randomUUID(), body(300));

        assertEquals(300, budget(1000, 0).shareUsed());
    }

    @Test
    void nothingIsAcceptedBelowTheFreeSpaceFloor() {
        StorageBudget budget = budget(1000, Long.MAX_VALUE);

        assertThrows(StorageFullException.class, () -> budget.requireShareRoom(1));
        assertThrows(StorageFullException.class, () -> budget.requireDiskRoom(0));
    }

    private StorageBudget budget(long shareTotal, long minFree) {
        return new StorageBudget(properties(shareTotal, minFree));
    }

    private StorageProperties properties(long shareTotal, long minFree) {
        return new StorageProperties(root.toString(), 10_000, 10_000, shareTotal, minFree);
    }

    private static ByteArrayInputStream body(int size) {
        return new ByteArrayInputStream(new byte[size]);
    }
}
