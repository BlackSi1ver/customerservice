package com.customerservice.exception;

public class NotFoundUserException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotFoundUserException(final String message) {
        super(message);
    }

    public NotFoundUserException() {
        super();
    }
}
