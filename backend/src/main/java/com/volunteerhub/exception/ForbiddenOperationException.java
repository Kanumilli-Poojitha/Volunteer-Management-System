package com.volunteerhub.exception;

/** The caller is authenticated but not allowed to perform this specific operation (HTTP 403). */
public class ForbiddenOperationException extends RuntimeException {
    public ForbiddenOperationException(String message) { super(message); }
}
