package com.blikeng.job.executor.exception.Http;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {
    private final static Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<String> handleApiException(ApiException apiException){
        logger.error("API Exception: {}", apiException.getMessage());
        return ResponseEntity.status(apiException.getStatusCode()).body(apiException.getMessage());
    }
}
