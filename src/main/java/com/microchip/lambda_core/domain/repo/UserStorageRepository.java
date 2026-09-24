package com.microchip.lambda_core.domain.repo;

import java.util.UUID;

import com.microchip.lambda_core.domain.UserStorage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserStorageRepository extends JpaRepository<UserStorage, UUID> {
}
