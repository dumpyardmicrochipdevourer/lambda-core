package com.microchip.lambda_core.service.exceptions;

import java.util.UUID;

public class ShareFileNotFoundException extends RuntimeException {

    public ShareFileNotFoundException(UUID fileId) {
        super("файл не найден: " + fileId);
    }
}
