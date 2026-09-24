package com.microchip.lambda_core.domain.dto;

import java.time.Instant;
import java.util.List;

public record ShareManifestView(String code, Instant expiresAt, List<ShareFileView> files) {

}
