package com.microchip.lambda_core.domain.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.microchip.lambda_core.domain.FileStatus;
import com.microchip.lambda_core.domain.UserFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserFileRepository extends JpaRepository<UserFile, UUID> {

    List<UserFile> findByOwnerIdAndStatus(UUID ownerId, FileStatus status);

    List<UserFile> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<UserFile> findByIdAndOwnerId(UUID id, UUID ownerId);

    List<UserFile> findByStatusAndCreatedAtBefore(FileStatus status, Instant instant);

    @Query("select lower(f.name) from UserFile f where f.ownerId = :ownerId")
    List<String> findLowerNames(@Param("ownerId") UUID ownerId);
}
