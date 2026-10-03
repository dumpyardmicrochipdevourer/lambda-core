package com.microchip.lambda_core.web;

import com.microchip.lambda_core.domain.dto.StartUploadRequest;
import com.microchip.lambda_core.domain.dto.StorageView;
import com.microchip.lambda_core.domain.dto.UserFileResource;
import com.microchip.lambda_core.domain.dto.UserFileView;
import com.microchip.lambda_core.service.UserFileService;
import com.microchip.lambda_core.service.exceptions.QuotaExceededException;
import com.microchip.lambda_core.service.exceptions.UploadOffsetMismatchException;
import com.microchip.lambda_core.service.exceptions.UserFileNotFoundException;
import com.microchip.lambda_core.storage.FileTooLargeException;
import com.microchip.lambda_core.storage.StorageFullException;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.UUID;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.core.io.support.ResourceRegion;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/files")
public class UserFileController {

    static final String UPLOAD_OFFSET = "Upload-Offset";

    private final UserFileService fileService;

    public UserFileController(UserFileService fileService) {
        this.fileService = fileService;
    }

    @GetMapping
    public StorageView list(@AuthenticationPrincipal Jwt jwt) {
        return fileService.list(owner(jwt), username(jwt));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserFileView start(@AuthenticationPrincipal Jwt jwt, @RequestBody StartUploadRequest request) {
        if (request.size() == null) {
            throw new IllegalArgumentException("size обязателен");
        }
        return fileService.start(owner(jwt), username(jwt), request.name(), request.size(), request.contentType());
    }

    @PutMapping("/{id}/content")
    public UserFileView append(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestHeader(value = UPLOAD_OFFSET, defaultValue = "0") long offset,
            HttpServletRequest request) throws IOException {
        return fileService.append(owner(jwt), id, offset, request.getInputStream());
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<ResourceRegion> download(
            @AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID id,
            @RequestHeader(value = HttpHeaders.RANGE, required = false) String range) {
        UserFileResource file = fileService.get(owner(jwt), id);
        return Downloads.region(file.path(), file.name(), file.contentType(), file.sizeBytes(), range);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID id) {
        fileService.delete(owner(jwt), id);
    }

    private static UUID owner(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }

    private static String username(Jwt jwt) {
        String name = jwt.getClaimAsString("username");
        return name != null ? name : jwt.getSubject().substring(0, 8);
    }

    @ExceptionHandler(UserFileNotFoundException.class)
    ProblemDetail handleNotFound(UserFileNotFoundException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    }

    @ExceptionHandler({QuotaExceededException.class, StorageFullException.class})
    ProblemDetail handleQuota(RuntimeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.INSUFFICIENT_STORAGE, e.getMessage());
    }

    @ExceptionHandler(UploadOffsetMismatchException.class)
    ResponseEntity<ProblemDetail> handleOffset(UploadOffsetMismatchException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, e.getMessage());
        problem.setProperty("offset", e.getExpected());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .header(UPLOAD_OFFSET, String.valueOf(e.getExpected()))
                .body(problem);
    }

    @ExceptionHandler(FileTooLargeException.class)
    ProblemDetail handleTooLarge(FileTooLargeException e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE, e.getMessage());
    }

    @ExceptionHandler({IllegalArgumentException.class, HttpMessageNotReadableException.class})
    ProblemDetail handleBadRequest(Exception e) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    }
}
