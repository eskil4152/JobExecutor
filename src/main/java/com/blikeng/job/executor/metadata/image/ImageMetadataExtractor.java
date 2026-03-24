package com.blikeng.job.executor.metadata.image;

import tools.jackson.databind.node.ObjectNode;

import java.nio.file.Path;

public class ImageMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        result.put("category", "image");
    }
}
