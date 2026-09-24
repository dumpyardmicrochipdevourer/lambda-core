package com.microchip.lambda_core.service.util;

import com.microchip.lambda_core.domain.Share;
import com.microchip.lambda_core.domain.repo.ShareRepository;
import com.microchip.lambda_core.storage.ShareStorage;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ShareCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(ShareCleanupJob.class);

    private final ShareRepository shareRepository;
    private final ShareStorage storage;

    public ShareCleanupJob(ShareRepository shareRepository, ShareStorage storage) {
        this.shareRepository = shareRepository;
        this.storage = storage;
    }

    @Scheduled(fixedDelay = 60_000)
    public void sweep() {
        List<Share> expired = shareRepository.findByExpiresAtBefore(Instant.now());
        for (Share share : expired) {
            shareRepository.delete(share);
            try {
                storage.deleteShareDir(share.getId());
            } catch (IOException e) {
                log.warn("не удалось удалить каталог share {}", share.getId(), e);
            }
        }
    }
}
