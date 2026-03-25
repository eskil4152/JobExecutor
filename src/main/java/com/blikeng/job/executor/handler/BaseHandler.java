package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.service.StorageService;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;

public abstract class BaseHandler {
    protected final ObjectMapper objectMapper;
    protected final StorageService storageService;

    protected BaseHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    protected <T> T parsePayload(String payloadString, Class<T> type, String context) {
        try {
            return objectMapper.readValue(payloadString, type);
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid payload for " + context, e);
        }
    }

    protected Path getFilePath(String fileId, String context) {
        try {
            return storageService.getPath(fileId);
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid fileId for " + context, e);
        }
    }
}
