package com.customerservice.exception;

public class ForbiddenAccessException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public ForbiddenAccessException(final String message) {
        super(message);
    }
}
