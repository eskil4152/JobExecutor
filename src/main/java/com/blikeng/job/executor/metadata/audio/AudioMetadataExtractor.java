package com.blikeng.job.executor.metadata.audio;

import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

public class AudioMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        result.put("category", "audio");
    }
}
