package com.microchip.lambda_core.storage;

public class StorageFullException extends RuntimeException {

    public StorageFullException() {
        super("хранилище заполнено, попробуйте позже");
    }
}
