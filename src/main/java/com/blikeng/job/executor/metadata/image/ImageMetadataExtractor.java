package com.blikeng.job.executor.metadata.image;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;

public class ImageMetadataExtractor {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode extract(Path path) throws IOException {
        ObjectNode result = objectMapper.createObjectNode();

        result.put("category", "image");

        return result;
    }
}
