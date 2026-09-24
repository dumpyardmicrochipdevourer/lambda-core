package com.microchip.lambda_core.domain.repo;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.microchip.lambda_core.domain.Share;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShareRepository extends JpaRepository<Share, UUID> {

    Optional<Share> findByCode(String code);

    List<Share> findByExpiresAtBefore(Instant instant);
}
