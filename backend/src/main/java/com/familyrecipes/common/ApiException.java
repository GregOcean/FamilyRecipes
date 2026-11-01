package com.familyrecipes.common;

public class ApiException extends RuntimeException {
    private final int status;
    public ApiException(int status, String message) { super(message); this.status = status; }
    public ApiException(int status, String message, Throwable cause) { super(message, cause); this.status = status; }
    public int getStatus() { return status; }
}
