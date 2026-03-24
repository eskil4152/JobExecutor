package com.blikeng.job.executor.metadata;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;

public class FileTypeExtractor {

    public static HashMap<String, String> findFileType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (contentType != null) {
            String[] parts = contentType.split("/");
            String fileType = parts.length > 0 ? parts[0] : "unknown";

            return switch (fileType) {
                case "text" -> textExtractor(path);
                case "application" -> applicationExtractor(path);
                case "image" -> imageExtractor(path);
                case "video" -> videoExtractor(path);
                case "audio" -> audioExtractor(path);
                default -> new HashMap<>();
            };
        }

        return new HashMap<>();
    }

    private static HashMap<String, String> textExtractor(Path path) {
        return new HashMap<>();
    }

    private static HashMap<String, String> applicationExtractor(Path path) {
        return new HashMap<>();
    }

    private static HashMap<String, String> imageExtractor(Path path) {
        return new HashMap<>();
    }

    private static HashMap<String, String> videoExtractor(Path path) {
        return new HashMap<>();
    }

    private static HashMap<String, String> audioExtractor(Path path) {
        return new HashMap<>();
    }
}
