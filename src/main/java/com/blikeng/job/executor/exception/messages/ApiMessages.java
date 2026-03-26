package com.blikeng.job.executor.exception.messages;

public enum ApiMessages {
    JOB_TYPE_AND_PAYLOAD_REQUIRED("Job type and payload are required"),
    JOB_CREATION_FAILED("Failed to create job from request payload"),
    JOB_NOT_FOUND("Requested job was not found"),
    JOB_READ_FAILED("Failed to read stored job data"),
    INVALID_UUID("Invalid UUID"),
    URL_NOT_VALID("URL not valid"),
    FILE_NOT_FOUND("File not found");

    private final String message;

    ApiMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
