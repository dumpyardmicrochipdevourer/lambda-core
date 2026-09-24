package com.microchip.lambda_core.service;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.Share;
import com.microchip.lambda_core.domain.ShareFile;
import com.microchip.lambda_core.domain.dto.ShareFileResource;
import com.microchip.lambda_core.domain.repo.ShareFileRepository;
import com.microchip.lambda_core.domain.repo.ShareRepository;
import com.microchip.lambda_core.service.exceptions.ShareFileNotFoundException;
import com.microchip.lambda_core.service.exceptions.ShareNotFoundException;
import com.microchip.lambda_core.service.util.ShareCodeGenerator;
import com.microchip.lambda_core.storage.ShareStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
public class ShareService {

    private static final int CODE_ATTEMPTS = 10;

    private final ShareRepository shareRepository;
    private final ShareFileRepository shareFileRepository;
    private final ShareStorage storage;
    private final ShareCodeGenerator codeGenerator;

    public ShareService(
            ShareRepository shareRepository,
            ShareFileRepository shareFileRepository,
            ShareStorage storage,
            ShareCodeGenerator codeGenerator) {
        this.shareRepository = shareRepository;
        this.shareFileRepository = shareFileRepository;
        this.storage = storage;
        this.codeGenerator = codeGenerator;
    }

    public Share createCode(Duration ttl) {
        for (int attempt = 0; attempt < CODE_ATTEMPTS; attempt++) {
            Share share = new Share(codeGenerator.generate(), Instant.now().plus(ttl));
            try {
                return shareRepository.saveAndFlush(share);
            } catch (DataIntegrityViolationException e) {
            }
        }
        throw new IllegalStateException("не удалось подобрать свободный код обмена");
    }

    public ShareFile uploadFile(String code, String name, String contentType, InputStream body) {
        Share share = liveShare(code);

        ShareFile file = shareFileRepository.save(new ShareFile(share.getId(), name, 0, contentType));

        long written;
        try {
            written = storage.write(share.getId(), file.getId(), body);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        file.markComplete(written);
        return shareFileRepository.save(file);
    }

    public Share get(String code) {
        return liveShare(code);
    }

    public List<ShareFile> listFiles(String code) {
        Share share = liveShare(code);
        return shareFileRepository.findByShareId(share.getId());
    }

    public ShareFileResource getFile(String code, UUID fileId) {
        Share share = liveShare(code);
        ShareFile file = shareFileRepository.findById(fileId)
                .filter(f -> f.getShareId().equals(share.getId()))
                .filter(f -> f.getStatus() == FileStatus.COMPLETE)
                .orElseThrow(() -> new ShareFileNotFoundException(fileId));

        return new ShareFileResource(
                storage.locate(share.getId(), file.getId()),
                file.getName(),
                file.getContentType(),
                file.getSizeBytes());
    }

    private Share liveShare(String code) {
        return shareRepository.findByCode(code)
                .filter(s -> s.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> new ShareNotFoundException(code));
    }
}
