package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.payloads.HashComparisonPayload;
import com.blikeng.job.executor.payloads.TextPayload;
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
    protected HashHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleFileHashing(String payloadString) {
        FilePayload payload = parsePayload(payloadString, FilePayload.class, "File Hashing");

        try {
            Path path = getFilePath(payload.fileId(), "File Hashing");

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (DigestInputStream digestStream = new DigestInputStream(Files.newInputStream(path), md)) {
                byte[] buffer = new byte[1024];
                while (digestStream.read(buffer) != -1) {
                    // read bytes until EOF
                }
            }

            String hex = HexFormat.of().formatHex(md.digest());

            return objectMapper.createObjectNode()
                    .put("hash", hex)
                    .put("algorithm", "SHA-256");

        } catch (IOException e) {
            throw new FileProcessingException("Unable to read file for File Hashing", e);

        } catch (NoSuchAlgorithmException exception) {
            throw new AlgorithmException("Algorithm was not found", "JobHandler.handleFileHashing", "SHA-256");
        }
    }

    public JsonNode handleTextHashing(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "Text Hashing");
        if (payload.content() == null || payload.content().isBlank()) {
            throw new InvalidPayloadException("Text content must not be null or empty", null);
        }

        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = payload.content().getBytes(StandardCharsets.UTF_8);
            byte[] digest = md.digest(bytes);

            String hex = HexFormat.of().formatHex(digest);

            return objectMapper.createObjectNode()
                    .put("hash", hex)
                    .put("algorithm", "SHA-256");

        } catch (NoSuchAlgorithmException exception) {
            throw new AlgorithmException("Algorithm was not found", "JobHandler.handleTextHashing", "SHA-256");
        }
    }

    public JsonNode handleHashComparison(String payloadString) {
        HashComparisonPayload payload = parsePayload(payloadString, HashComparisonPayload.class, "Hash Comparison");

        if (payload.hashA() == null || payload.hashB() == null) {
            throw new InvalidPayloadException("HashA and HashB must not be null", null);
        }

        return objectMapper.createObjectNode()
                .put("match", payload.hashA().equals(payload.hashB()));
    }
}
