package com.blikeng.job.executor.exception;

public class FileProcessingException extends RuntimeException {
    private final String location;

    public FileProcessingException(String message, String location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
