package com.proxio.backend.exceptions;

public class DeleteOperationException extends RuntimeException {
    public DeleteOperationException(String message) {
        super(message);
    }
}
