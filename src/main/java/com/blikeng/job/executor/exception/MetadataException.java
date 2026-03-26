package com.blikeng.job.executor.exception;

public class MetadataException extends RuntimeException {
    private final String location;

    public MetadataException(String message, String location, Throwable cause) {
        super(message, cause);
        this.location = location;
    }

    public String getLocation() {
        return location;
    }
}
