package com.microchip.lambda_core.domain.repo;

import java.util.List;
import java.util.UUID;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

    List<UserFile> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);
}
