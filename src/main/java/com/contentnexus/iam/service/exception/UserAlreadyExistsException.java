package com.contentnexus.iam.service.exception;

public class UserAlreadyExistsException  extends RuntimeException {
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
