package com.microchip.lambda_core.domain.dto;

import com.microchip.lambda_core.domain.UserFile;
import java.time.Instant;
import java.util.UUID;

public record UserFileView(UUID id, String name, long size, long received, String status, Instant createdAt) {

    public static UserFileView of(UserFile file) {
        return new UserFileView(file.getId(), file.getName(), file.getSizeBytes(), file.getReceivedBytes(),
                file.getStatus().name(), file.getCreatedAt());
    }
}
