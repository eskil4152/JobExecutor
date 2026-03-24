package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.metadata.FileTypeExtractor;
import com.blikeng.job.executor.metadata.GeneralMetadata;
import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.payloads.CountWordsPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class JobHandler {
    private final ObjectMapper objectMapper;
    private final StorageService storageService;

    public JobHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public JsonNode handleCountWords(String payloadString) {
        CountWordsPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, CountWordsPayload.class);
        } catch (Exception e) {
            throw new InvalidPayloadException("Invalid payload at CountWords", e);
        }

        String text = payload.words();
        String[] words = text.trim().split("\\s+");

        return objectMapper.createObjectNode()
                .put("words", words.length);
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
                    .put("words", words)
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
}
