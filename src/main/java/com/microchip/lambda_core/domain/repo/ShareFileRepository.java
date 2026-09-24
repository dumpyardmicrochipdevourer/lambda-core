package com.microchip.lambda_core.domain.repo;

import java.util.List;
import java.util.UUID;

import com.microchip.lambda_core.domain.ShareFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareFileRepository extends JpaRepository<ShareFile, UUID> {

    List<ShareFile> findByShareId(UUID shareId);
}
