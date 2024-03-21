package com.customerservice.exception;

public class InvalidStateException extends RuntimeException{
    private static final long serialVersionUID = 1L;

    public InvalidStateException(final String message) {
        super(message);
    }
}
