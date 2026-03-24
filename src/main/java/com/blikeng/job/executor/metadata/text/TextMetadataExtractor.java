package com.blikeng.job.executor.metadata.text;

import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

public class TextMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        result.put("category", "text");
    }
}
