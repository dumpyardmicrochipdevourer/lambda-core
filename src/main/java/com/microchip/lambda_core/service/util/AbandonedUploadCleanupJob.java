package com.microchip.lambda_core.service.util;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.UserFile;
import com.microchip.lambda_core.domain.repo.UserFileRepository;
import com.microchip.lambda_core.service.UserFileService;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AbandonedUploadCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(AbandonedUploadCleanupJob.class);
    private static final Duration KEEP = Duration.ofDays(7);

    private final UserFileRepository fileRepository;
    private final UserFileService fileService;

    public AbandonedUploadCleanupJob(UserFileRepository fileRepository, UserFileService fileService) {
        this.fileRepository = fileRepository;
        this.fileService = fileService;
    }

    // a half-uploaded file holds its whole size in reserve, so a forgotten one would eat the quota forever
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT5M")
    public void sweep() {
        for (UserFile file : fileRepository.findByStatusAndCreatedAtBefore(FileStatus.UPLOADING, Instant.now().minus(KEEP))) {
            try {
                fileService.delete(file.getOwnerId(), file.getId());
            } catch (RuntimeException e) {
                log.warn("не удалось убрать брошенную заливку {}", file.getId(), e);
            }
        }
    }
}
