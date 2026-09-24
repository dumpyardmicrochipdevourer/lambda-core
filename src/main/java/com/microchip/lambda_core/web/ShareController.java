package com.microchip.lambda_core.web;

import com.microchip.lambda_core.domain.Share;
import com.microchip.lambda_core.domain.ShareFile;
import com.microchip.lambda_core.domain.dto.CreateShareRequest;
import com.microchip.lambda_core.domain.dto.ShareFileView;
import com.microchip.lambda_core.domain.dto.ShareFileResource;
import com.microchip.lambda_core.domain.dto.ShareManifestView;
import com.microchip.lambda_core.domain.dto.ShareView;
import com.microchip.lambda_core.service.ShareService;
import com.microchip.lambda_core.service.exceptions.ShareFileNotFoundException;
import com.microchip.lambda_core.service.exceptions.ShareNotFoundException;
import com.microchip.lambda_core.storage.FileTooLargeException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRange;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/share")
public class ShareController {

    private static final Set<Long> ALLOWED_TTL_SECONDS = Set.of(900L, 3600L, 86400L);

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @PostMapping
    public ShareView createCode(@RequestBody CreateShareRequest request) {
        if (!ALLOWED_TTL_SECONDS.contains(request.ttlSeconds())) {
            throw new IllegalArgumentException("ttlSeconds должен быть одним из " + ALLOWED_TTL_SECONDS);
        }
        Share share = shareService.createCode(Duration.ofSeconds(request.ttlSeconds()));
        return new ShareView(share.getCode(), share.getExpiresAt());
    }

    @PutMapping("/{code}/files/{name}")
    @ResponseStatus(HttpStatus.CREATED)
    public ShareFileView upload(
            @PathVariable String code,
            @PathVariable String name,
            @RequestHeader(value = HttpHeaders.CONTENT_TYPE, required = false) String contentType,
            HttpServletRequest request) throws IOException {
        ShareFile file = shareService.uploadFile(code, name, contentType, request.getInputStream());
        return new ShareFileView(file.getId(), file.getName(), file.getSizeBytes());
    }

    @GetMapping("/{code}")
    public ShareManifestView manifest(@PathVariable String code) {
        Share share = shareService.get(code);
        List<ShareFileView> files = shareService.listFiles(code).stream()
                .map(f -> new ShareFileView(f.getId(), f.getName(), f.getSizeBytes()))
                .toList();
        return new ShareManifestView(share.getCode(), share.getExpiresAt(), files);
    }

    @GetMapping("/{code}/files/{fileId}")
    public ResponseEntity<ResourceRegion> download(
            @PathVariable String code,
            @PathVariable UUID fileId,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String rangeHeader) {
        ShareFileResource file = shareService.getFile(code, fileId);
        FileSystemResource resource = new FileSystemResource(file.path());

        ResourceRegion region;
        HttpStatus status;
        if (rangeHeader != null) {
            HttpRange range = HttpRange.parseRanges(rangeHeader).get(0);
            region = range.toResourceRegion(resource);
            status = HttpStatus.PARTIAL_CONTENT;
        } else {
            region = new ResourceRegion(resource, 0, file.sizeBytes());
            status = HttpStatus.OK;
        }

        String contentType = file.contentType() != null ? file.contentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        String disposition = ContentDisposition.attachment().filename(file.name(), StandardCharsets.UTF_8)
                .build().toString();

        return ResponseEntity.status(status)
                .header(HttpHeaders.ACCEPT_RANGES, "bytes")
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .header("X-Content-Type-Options", "nosniff")
                .body(region);
    }

    @ExceptionHandler(ShareNotFoundException.class)
    ProblemDetail handleNotFound(ShareNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(ShareFileNotFoundException.class)
    ProblemDetail handleFileNotFound(ShareFileNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ProblemDetail handleBadRequest(IllegalArgumentException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    @ExceptionHandler(FileTooLargeException.class)
    ProblemDetail handleTooLarge(FileTooLargeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }
}
