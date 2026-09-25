package com.microchip.lambda_core.domain.dto;

import java.nio.file.Path;

public record UserFileResource(Path path, String name, String contentType, long sizeBytes) {
}
