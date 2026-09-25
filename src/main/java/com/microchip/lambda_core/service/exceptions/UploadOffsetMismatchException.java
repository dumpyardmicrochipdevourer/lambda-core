package com.microchip.lambda_core.service.exceptions;

public class UploadOffsetMismatchException extends RuntimeException {

    private final long expected;

    public UploadOffsetMismatchException(long expected) {
        super("заливка продолжается с байта " + expected);
        this.expected = expected;
    }

    public long getExpected() { return expected; }
}
