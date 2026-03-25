package com.blikeng.job.executor.payloads;

public record HashPayload (
        String fileId,
        String content,
        String algorithm
){}
