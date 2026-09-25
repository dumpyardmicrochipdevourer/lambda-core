package com.microchip.lambda_core.domain.dto;

public record StartUploadRequest(String name, Long size, String contentType) {
}
