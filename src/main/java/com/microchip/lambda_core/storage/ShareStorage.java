package com.microchip.lambda_core.storage;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class ShareStorage {

    private final Path root;

    public ShareStorage(StorageProperties properties) {
        this.root = Path.of(properties.root()).resolve("share");
    }

    public long write(UUID shareId, UUID fileId, InputStream body) throws IOException {
        Path target = locate(shareId, fileId);
        Files.createDirectories(target.getParent());
        try (OutputStream out = Files.newOutputStream(
                target, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)) {
            return body.transferTo(out);
        }
    }

    public Path locate(UUID shareId, UUID fileId) {
        return root.resolve(shareId.toString()).resolve(fileId.toString());
    }
}
