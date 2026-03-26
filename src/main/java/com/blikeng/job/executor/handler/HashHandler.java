package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import com.blikeng.job.executor.payloads.HashComparisonPayload;
import com.blikeng.job.executor.payloads.HashPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class HashHandler extends BaseHandler {
    public HashHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleFileHashing(String payloadString) {
        HashPayload payload = parsePayload(payloadString, HashPayload.class, "File Hashing");

        String algorithm = (payload.algorithm() == null || payload.algorithm().isBlank())
                ? "SHA-256"
                : payload.algorithm().trim().toUpperCase();

        try {
            Path path = getFilePath(payload.fileId(), "File Hashing");

            MessageDigest md = MessageDigest.getInstance(algorithm);

            try (DigestInputStream digestStream = new DigestInputStream(Files.newInputStream(path), md)) {
                byte[] buffer = new byte[1024];
                while (digestStream.read(buffer) != -1) {
                    // read bytes until EOF
                }
            }

            String hex = HexFormat.of().formatHex(md.digest());

            return objectMapper.createObjectNode()
                    .put("hash", hex)
                    .put("algorithm", algorithm);

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_READ_FILE.getMessage(), "HashHandler.handleFileHashing", e);

        } catch (NoSuchAlgorithmException exception) {
            throw new AlgorithmException(InternalMessages.ALGORITHM_NOT_FOUND.getMessage(), "JobHandler.handleFileHashing", algorithm, exception);
        }
    }

    public JsonNode handleTextHashing(String payloadString) {
        HashPayload payload = parsePayload(payloadString, HashPayload.class, "Text Hashing");

        String algorithm = (payload.algorithm() == null || payload.algorithm().isBlank())
                ? "SHA-256"
                : payload.algorithm().trim().toUpperCase();

        if (payload.content() == null || payload.content().isBlank()) {
            throw new InvalidPayloadException(InternalMessages.INVALID_TEXT_CONTENT.getMessage(), "HashHandler.handleTextHashing", null);
        }

        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] bytes = payload.content().getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(bytes);

            String hex = HexFormat.of().formatHex(digest);

            return objectMapper.createObjectNode()
                    .put("hash", hex)
                    .put("algorithm", algorithm);

        } catch (NoSuchAlgorithmException exception) {
            throw new AlgorithmException(InternalMessages.ALGORITHM_NOT_FOUND.getMessage(), "JobHandler.handleTextHashing", algorithm, exception);
        }
    }

    public JsonNode handleHashComparison(String payloadString) {
        HashComparisonPayload payload = parsePayload(payloadString, HashComparisonPayload.class, "Hash Comparison");

        if (payload.hashA() == null || payload.hashA().isBlank()
                || payload.hashB() == null || payload.hashB().isBlank()) {
            throw new InvalidPayloadException(
                    InternalMessages.INVALID_HASH_INPUTS.getMessage(),
                    "HashHandler.handleHashComparison",
                    null
            );
        }

        return objectMapper.createObjectNode()
                .put("match", payload.hashA().equals(payload.hashB()));
    }
}
