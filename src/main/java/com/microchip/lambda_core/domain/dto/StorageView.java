package com.microchip.lambda_core.domain.dto;

import java.util.List;

public record StorageView(long quota, long used, long reserved, List<UserFileView> files) {
}
