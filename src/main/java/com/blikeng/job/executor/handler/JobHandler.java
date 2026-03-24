package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.AnalyzeFilePayload;
import com.blikeng.job.executor.payloads.CountWordsPayload;
import com.blikeng.job.executor.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class JobHandler {
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(JobHandler.class);
    private final StorageService storageService;

    public JobHandler(ObjectMapper objectMapper, StorageService storageService) {
        this.objectMapper = objectMapper;
        this.storageService = storageService;
    }

    public String handleCountWords(String payloadString) {
        CountWordsPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, CountWordsPayload.class);
        } catch (Exception e) {
            logger.error("Unable to read payload for CountWords: {}", payloadString);
            throw new RuntimeException("Invalid payload");
        }

        String text = payload.words();
        String[] words = text.trim().split("\\s+");

        return "Total words: " + words.length;
    }

    public String handleAddNumbers(String payloadString) {
        AddNumbersPayload payload;

        try {
             payload = objectMapper.readValue(payloadString, AddNumbersPayload.class);
        } catch (Exception e) {
            logger.error("Unable to read payload for AddNumbers: {}", payloadString);
            throw new RuntimeException("Invalid payload");
        }

        return "Sum = " + (payload.a() + payload.b());
    }

    public String handleFileAnalysis(String payloadString) throws IOException {
        AnalyzeFilePayload payload;

        try {
            payload = objectMapper.readValue(payloadString, AnalyzeFilePayload.class);
        } catch (Exception e) {
            logger.error("Unable to read payload for FileAnalysis: {}", payloadString);
            throw new RuntimeException("Invalid payload");
        }

        Path path = storageService.getPath(payload.fileId());
        String content = Files.readString(path);
        int words = content.trim().split("\\s+").length;

        // More stuff

        return "";
    }
}
