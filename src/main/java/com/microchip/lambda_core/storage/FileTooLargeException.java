package com.microchip.lambda_core.storage;

public class FileTooLargeException extends RuntimeException {

    public FileTooLargeException(long maxBytes) {
        super("файл больше допустимого предела: " + maxBytes + " байт");
    }
}
