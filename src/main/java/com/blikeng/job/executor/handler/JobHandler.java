package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.payloads.AddNumbersPayload;
import com.blikeng.job.executor.payloads.TextPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Service
public class JobHandler extends BaseHandler {
    public JobHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleCountWords(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "CountWords");

        String text = payload.content();
        if (text == null || text.isBlank()){
            return objectMapper.createObjectNode()
                    .put("words", 0);
        }

        int words = text.trim().split("\\s+").length;

        return objectMapper.createObjectNode()
                .put("words", words);
    }

    public JsonNode handleAddNumbers(String payloadString) {
        AddNumbersPayload payload = parsePayload(payloadString, AddNumbersPayload.class, "AddNumbers");

        return objectMapper.createObjectNode()
                .put("sum", payload.a() + payload.b());
    }
}
