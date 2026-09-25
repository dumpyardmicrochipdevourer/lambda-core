package com.microchip.lambda_core.service.exceptions;

public class QuotaExceededException extends RuntimeException {

    public QuotaExceededException(long free) {
        super("не хватает места, свободно " + free + " байт");
    }
}
