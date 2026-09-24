package com.microchip.lambda_core.domain.repo;

import java.util.List;

import com.microchip.lambda_core.domain.FeedbackMessage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FeedbackMessageRepository extends JpaRepository<FeedbackMessage, Long> {

    List<FeedbackMessage> findByReadAtIsNull();
}
