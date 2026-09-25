package com.microchip.lambda_core.service;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.UserFile;
import com.microchip.lambda_core.domain.UserStorage;
import com.microchip.lambda_core.domain.dto.StorageView;
import com.microchip.lambda_core.domain.dto.UserFileResource;
import com.microchip.lambda_core.domain.dto.UserFileView;
import com.microchip.lambda_core.domain.repo.UserFileRepository;
import com.microchip.lambda_core.domain.repo.UserStorageRepository;
import com.microchip.lambda_core.service.exceptions.QuotaExceededException;
import com.microchip.lambda_core.service.exceptions.UploadOffsetMismatchException;
import com.microchip.lambda_core.service.exceptions.UserFileNotFoundException;
import com.microchip.lambda_core.service.util.FileNames;
import com.microchip.lambda_core.storage.StorageProperties;
import com.microchip.lambda_core.storage.UserFileStorage;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

@Service
public class UserFileService {

    private final UserStorageRepository storageRepository;
    private final UserFileRepository fileRepository;
    private final UserFileStorage storage;
    private final StorageProperties properties;
    private final TransactionTemplate tx;
    private final ConcurrentHashMap<UUID, ReentrantLock> uploadLocks = new ConcurrentHashMap<>();

    public UserFileService(
            UserStorageRepository storageRepository,
            UserFileRepository fileRepository,
            UserFileStorage storage,
            StorageProperties properties,
            PlatformTransactionManager transactionManager) {
        this.storageRepository = storageRepository;
        this.fileRepository = fileRepository;
        this.storage = storage;
        this.properties = properties;
        this.tx = new TransactionTemplate(transactionManager);
    }

    public StorageView list(UUID ownerId, String username) {
        UserStorage account = account(ownerId, username);
        return new StorageView(account.getQuotaBytes(), account.getUsedBytes(), account.getReservedBytes(),
                fileRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(UserFileView::of).toList());
    }

    public UserFileView start(UUID ownerId, String username, String name, long size, String contentType) {
        String clean = FileNames.validate(name);
        if (size < 0) {
            throw new IllegalArgumentException("размер не может быть отрицательным");
        }
        return tx.execute(status -> {
            UserStorage account = account(ownerId, username);
            if (storageRepository.reserve(ownerId, size) == 0) {
                throw new QuotaExceededException(
                        Math.max(0, account.getQuotaBytes() - account.getUsedBytes() - account.getReservedBytes()));
            }
            String finalName = FileNames.free(clean, new HashSet<>(fileRepository.findLowerNames(ownerId)));
            UserFile file = fileRepository.save(new UserFile(ownerId, finalName, size, contentType));
            if (size == 0) {
                file.setStatus(FileStatus.COMPLETE);
                touch(ownerId, file.getId());
            }
            return UserFileView.of(file);
        });
    }

    public UserFileView append(UUID ownerId, UUID fileId, long offset, InputStream body) {
        ReentrantLock lock = uploadLocks.computeIfAbsent(fileId, id -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new UploadOffsetMismatchException(received(ownerId, fileId));
        }
        try {
            UserFile file = uploading(ownerId, fileId);
            long have = storage.sizeOnDisk(ownerId, fileId);
            if (offset != have) {
                throw new UploadOffsetMismatchException(have);
            }
            try {
                storage.append(ownerId, fileId, body, file.getSizeBytes());
            } finally {
                long now = storage.sizeOnDisk(ownerId, fileId);
                tx.executeWithoutResult(s -> finish(fileId, now));
            }
            return UserFileView.of(fileRepository.findById(fileId).orElseThrow());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } finally {
            lock.unlock();
            uploadLocks.remove(fileId, lock);
        }
    }

    public UserFileResource get(UUID ownerId, UUID fileId) {
        UserFile file = fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .filter(f -> f.getStatus() == FileStatus.COMPLETE)
                .orElseThrow(() -> new UserFileNotFoundException(fileId));
        return new UserFileResource(storage.locate(ownerId, fileId), file.getName(), file.getContentType(),
                file.getSizeBytes());
    }

    public void delete(UUID ownerId, UUID fileId) {
        UserFile file = tx.execute(s -> {
            UserFile f = fileRepository.findByIdAndOwnerId(fileId, ownerId)
                    .orElseThrow(() -> new UserFileNotFoundException(fileId));
            fileRepository.delete(f);
            if (f.getStatus() == FileStatus.COMPLETE) {
                storageRepository.free(ownerId, f.getSizeBytes());
            } else {
                storageRepository.release(ownerId, f.getSizeBytes());
            }
            return f;
        });
        try {
            storage.delete(ownerId, file.getId());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void finish(UUID fileId, long received) {
        UserFile file = fileRepository.findById(fileId).orElseThrow(() -> new UserFileNotFoundException(fileId));
        file.setReceivedBytes(received);
        if (received == file.getSizeBytes() && file.getStatus() == FileStatus.UPLOADING) {
            file.setStatus(FileStatus.COMPLETE);
            storageRepository.commit(file.getOwnerId(), file.getSizeBytes());
        }
        fileRepository.save(file);
    }

    private UserFile uploading(UUID ownerId, UUID fileId) {
        UserFile file = fileRepository.findByIdAndOwnerId(fileId, ownerId)
                .orElseThrow(() -> new UserFileNotFoundException(fileId));
        if (file.getStatus() != FileStatus.UPLOADING) {
            throw new UploadOffsetMismatchException(file.getSizeBytes());
        }
        return file;
    }

    private long received(UUID ownerId, UUID fileId) {
        try {
            return storage.sizeOnDisk(ownerId, fileId);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private void touch(UUID ownerId, UUID fileId) {
        try {
            storage.append(ownerId, fileId, InputStream.nullInputStream(), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private UserStorage account(UUID ownerId, String username) {
        return tx.execute(s -> {
            storageRepository.upsert(ownerId, username, properties.quotaDefaultBytes());
            return storageRepository.findById(ownerId).orElseThrow();
        });
    }
}
