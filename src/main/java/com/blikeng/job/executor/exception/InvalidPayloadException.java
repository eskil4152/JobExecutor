package com.blikeng.job.executor.exception;

public class InvalidPayloadException extends RuntimeException {
    private final String location;

  public InvalidPayloadException(String message, String location, Throwable cause) {
      super(message);
      this.location = location;
  }

  public String getLocation() {
      return location;
  }
}
