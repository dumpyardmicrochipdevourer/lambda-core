package com.microchip.lambda_core.service.exceptions;

public class ShareNotFoundException extends RuntimeException {

    public ShareNotFoundException(String code) {
        super("код не найден или истёк: " + code);
    }
}
