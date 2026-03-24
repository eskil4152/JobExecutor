package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.CountWordsPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

@Service
public class JobHandler {
    private final ObjectMapper objectMapper;
    private static final Logger logger = LoggerFactory.getLogger(JobHandler.class);

    public JobHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String handleCountWords(String payloadString) throws Exception {
        CountWordsPayload payload;

        try {
            payload = objectMapper.readValue(payloadString, CountWordsPayload.class);
        } catch (Exception e) {
            logger.error("Unable to read payload for CountWords: {}", payloadString);
            throw new RuntimeException("Invalid payload");
        }

        String text = payload.words();
        String[] words = text.trim().split("\\s+");

        return String.valueOf(words.length);
    }

    public String handleAddNumbers(String payloadString) {
        AddNumbersPayload payload;

        try {
             payload = objectMapper.readValue(payloadString, AddNumbersPayload.class);
        } catch (Exception e) {
            logger.error("Unable to read payload for AddNumbers: {}", payloadString);
            throw new RuntimeException("Invalid payload");
        }

        return String.valueOf(payload.a() + payload.b());
    }
}
