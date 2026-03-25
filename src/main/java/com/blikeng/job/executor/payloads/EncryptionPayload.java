package com.blikeng.job.executor.payloads;

public record EncryptionPayload (
        String content,
        String key
){}
