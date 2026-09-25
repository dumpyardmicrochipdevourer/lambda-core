package com.microchip.lambda_core.web;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

final class Downloads {

    private Downloads() {}

    static ResponseEntity<ResourceRegion> region(Path path, String name, String contentType, long size, String range) {
        FileSystemResource resource = new FileSystemResource(path);

        ResourceRegion region;
        HttpStatus status;
        if (range != null) {
            region = HttpRange.parseRanges(range).get(0).toResourceRegion(resource);
            status = HttpStatus.PARTIAL_CONTENT;
        } else {
            region = new ResourceRegion(resource, 0, size);
            status = HttpStatus.OK;
        }

        String type = contentType != null ? contentType : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String disposition = ContentDisposition.attachment().filename(name, StandardCharsets.UTF_8).build().toString();

        return ResponseEntity.status(status)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_TYPE, type)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(region);
    }
}
