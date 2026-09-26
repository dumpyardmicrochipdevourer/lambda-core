package com.microchip.lambda_core.domain.dto;

import com.microchip.lambda_core.domain.FeedbackMessage;
import java.time.Instant;

public record FeedbackView(long id, String body, Instant createdAt, Instant readAt) {

    public static FeedbackView of(FeedbackMessage message) {
        return new FeedbackView(message.getId(), message.getBody(), message.getCreatedAt(), message.getReadAt());
    }
}
