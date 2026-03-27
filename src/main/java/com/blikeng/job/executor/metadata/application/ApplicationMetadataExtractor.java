package com.blikeng.job.executor.metadata.application;

import tools.jackson.databind.node.ObjectNode;

public class ApplicationMetadataExtractor {

    private ApplicationMetadataExtractor() {
        /* This utility class should not be instantiated */
    }

    public static void extract(ObjectNode result) {
        result.put("category", "application");
    }
}
