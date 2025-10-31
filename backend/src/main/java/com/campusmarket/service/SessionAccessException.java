package com.campusmarket.service;

/**
 * Indicates the application could not access the backing session store (e.g. Redis).
 * Allows callers to gracefully fall back to stateless JWT authentication.
 */
public class SessionAccessException extends RuntimeException {
    public SessionAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
