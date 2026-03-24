package com.blikeng.job.executor.metadata.application;

import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

public class ApplicationMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        result.put("category", "application");
    }
}
