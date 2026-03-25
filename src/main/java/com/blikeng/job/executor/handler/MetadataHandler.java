package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.metadata.FileTypeExtractor;
import com.blikeng.job.executor.metadata.GeneralMetadata;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

@Service
public class MetadataHandler extends BaseHandler {
    protected MetadataHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleMetadataExtraction(String payloadString) {
        FilePayload payload = parsePayload(payloadString, FilePayload.class, "Metadata Extraction");

        Path path = getFilePath(payload.fileId(), "Metadata Extraction");

        ObjectNode result = objectMapper.createObjectNode();
        GeneralMetadata.getGeneralData(path, result);
        FileTypeExtractor.findFileType(path, result);

        return result;
    }
}
