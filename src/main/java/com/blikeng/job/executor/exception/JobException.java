package com.blikeng.job.executor.exception;

public class JobException extends RuntimeException {
    private final String id;

    public JobException(String message, String id) {
        super(message);
        this.id = id;
    }
}
