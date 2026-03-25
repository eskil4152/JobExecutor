package com.blikeng.job.executor.payloads;

public record DecryptionPayload(
        String fileId,
        String content,
        String key,
        String iv
) {}
