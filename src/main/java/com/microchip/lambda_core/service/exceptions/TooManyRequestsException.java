package com.microchip.lambda_core.service.exceptions;

public class TooManyRequestsException extends RuntimeException {

    public TooManyRequestsException() {
        super("слишком часто, попробуйте позже");
    }
}
