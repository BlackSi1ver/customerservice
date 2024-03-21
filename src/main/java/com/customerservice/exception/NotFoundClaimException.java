package com.customerservice.exception;

public class NotFoundClaimException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public NotFoundClaimException(final String message) {
        super(message);
    }

    public NotFoundClaimException() {
        super();
    }
}
