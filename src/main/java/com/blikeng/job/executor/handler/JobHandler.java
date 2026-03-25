package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.AlgorithmException;
import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.metadata.FileTypeExtractor;
import com.blikeng.job.executor.metadata.GeneralMetadata;
import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.payloads.HashComparisonPayload;
import com.blikeng.job.executor.payloads.TextPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.DigestInputStream;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

@Service
public class JobHandler {
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public JobHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public JsonNode handleCountWords(String payloadString) {
        TextPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, TextPayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid payload at CountWords", e);
        }

        String text = payload.content();
        String[] words = text.trim().split("\\s+");

        return objectMapper.createObjectNode()
                .put("content", words.length);
    }

    public JsonNode handleAddNumbers(String payloadString) {
        AddNumbersPayload payload;

        try {
             payload = objectMapper.readValue(payloadString, AddNumbersPayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Unable to read payload for Add Numbers", e);
        }

        return objectMapper.createObjectNode()
                .put("sum", payload.a() + payload.b());
    }

    public JsonNode handleFileAnalysis(String payloadString) {
        FilePayload payload;

        try {
            payload = objectMapper.readValue(payloadString, FilePayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Unable to read payload for File Analysis", e);
        }

        try {
            Path path = storageService.getPath(payload.fileId());
            String content = Files.readString(path);

            int words = content.trim().split("\\s+").length;
            int bytes = content.getBytes().length;
            int lines = content.split("\n").length;
            int characters = content.length();

            return objectMapper.createObjectNode()
                    .put("content", words)
                    .put("bytes", bytes)
                    .put("lines", lines)
                    .put("characters", characters)
                    ;
        } catch (IOException e) {
            throw new FileProcessingException("Unable to read file for File Analysis", e);
        }
    }

    public JsonNode handleMetadataExtraction(String payloadString) {
        FilePayload payload;

        try {
            payload = objectMapper.readValue(payloadString, FilePayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid payload for Metadata Extraction", e);
        }

        Path path = storageService.getPath(payload.fileId());
        ObjectNode result = objectMapper.createObjectNode();

        GeneralMetadata.getGeneralData(path, result);
        FileTypeExtractor.findFileType(path, result);

        return result;
    }

    public JsonNode handleFileHashing(String payloadString) {
        FilePayload payload;

        try {
            payload = objectMapper.readValue(payloadString, FilePayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Unable to read payload for File Hashing", e);
        }

        try {
            Path path = storageService.getPath(payload.fileId());

            MessageDigest md = MessageDigest.getInstance("SHA-256");

            try (DigestInputStream digestStream = new DigestInputStream(Files.newInputStream(path), md)) {
                byte[] buffer = new byte[1024];
                while (digestStream.read(buffer) != -1) {
                    // read the bytes until EOF
                }
            }

            String hex = HexFormat.of().formatHex(md.digest());

            return objectMapper.createObjectNode()
                    .put("hash", hex)
                    .put("algorithm", "SHA-256");

        } catch (IOException e) {
            throw new FileProcessingException("Unable to read file for File Analysis", e);

        } catch (NoSuchAlgorithmException exception) {
            throw new AlgorithmException("Algorithm was not found", "JobHandler.handleFileHashing", "SHA-256");
        }
    }

    public JsonNode handleTextHashing(String payloadString) {
        TextPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, TextPayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Unable to read payload for Text Hashing", e);
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
        HashComparisonPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, HashComparisonPayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Unable to read payload for Hash Comparison", e);
        }

        if (payload.hashA() == null || payload.hashB() == null) {
            throw new InvalidPayloadException("HashA and HashB must not be null", null);
        }

        return objectMapper.createObjectNode()
                .put("match", payload.hashA().equals(payload.hashB()));
    }
}
