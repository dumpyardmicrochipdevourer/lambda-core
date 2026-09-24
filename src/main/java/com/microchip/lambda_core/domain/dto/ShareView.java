package com.microchip.lambda_core.domain.dto;

import java.time.Instant;

public record ShareView(String code, Instant expiresAt) {

}
