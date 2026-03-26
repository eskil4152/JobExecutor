package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Service
public class FileAnalysisHandler extends BaseHandler {
    public FileAnalysisHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleFileAnalysis(String payloadString) {
        FilePayload payload = parsePayload(payloadString, FilePayload.class, "File Analysis");

        Path path = getFilePath(payload.fileId(), "File Analysis");
        String content = readFileContent(path);

        int words = content.isBlank() ? 0 : content.trim().split("\\s+").length;
        int lines = content.isEmpty() ? 0 : content.split("\\R").length;
        int characters = content.length();
        int bytes = content.getBytes(StandardCharsets.UTF_8).length;

        return objectMapper.createObjectNode()
                .put("words", words)
                .put("lines", lines)
                .put("characters", characters)
                .put("bytes", bytes);
    }

    private String readFileContent(Path path) {
        try {
            return Files.readString(path);
        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_READ_FILE.getMessage(), "FileAnalysisHandler.readFileContent", e);
        }
    }
}
