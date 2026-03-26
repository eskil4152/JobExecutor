package com.blikeng.job.executor.exception;

public class AlgorithmException extends RuntimeException {
    private final String algorithm;
    private final String location;

    public AlgorithmException(String message, String location, String algorithm, Throwable cause) {
        super(message, cause);
        this.algorithm = algorithm;
        this.location = location;
    }

    public String getAlgorithm() {
        return algorithm;
    }

    public String getLocation() {
        return location;
    }
}
