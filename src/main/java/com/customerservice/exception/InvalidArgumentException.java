package com.customerservice.exception;

public class InvalidArgumentException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public InvalidArgumentException(final String message) {
        super(message);
    }
}
