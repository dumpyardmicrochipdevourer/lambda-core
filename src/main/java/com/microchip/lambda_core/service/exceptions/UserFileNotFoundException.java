package com.microchip.lambda_core.service.exceptions;

import java.util.UUID;

public class UserFileNotFoundException extends RuntimeException {

    public UserFileNotFoundException(UUID fileId) {
        super("файл не найден: " + fileId);
    }
}
