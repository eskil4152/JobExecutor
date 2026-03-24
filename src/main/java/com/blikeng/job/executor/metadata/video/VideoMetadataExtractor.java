package com.blikeng.job.executor.metadata.video;

import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

public class VideoMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        result.put("category", "video");
    }
}
