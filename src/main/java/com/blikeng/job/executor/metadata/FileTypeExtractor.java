package com.blikeng.job.executor.metadata;

import com.blikeng.job.executor.metadata.application.ApplicationMetadataExtractor;
import com.blikeng.job.executor.metadata.audio.AudioMetadataExtractor;
import com.blikeng.job.executor.metadata.image.ImageMetadataExtractor;
import com.blikeng.job.executor.metadata.text.TextMetadataExtractor;
import com.blikeng.job.executor.metadata.video.VideoMetadataExtractor;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileTypeExtractor {

    public static ObjectNode findFileType(Path path) throws IOException {
        String contentType = Files.probeContentType(path);
        if (contentType != null) {
            String[] parts = contentType.split("/");
            String fileType = parts.length > 0 ? parts[0] : "unknown";

            return switch (fileType) {
                case "text" -> TextMetadataExtractor.extract(path);
                case "application" -> ApplicationMetadataExtractor.extract(path);
                case "image" -> ImageMetadataExtractor.extract(path);
                case "video" -> VideoMetadataExtractor.extract(path);
                case "audio" -> AudioMetadataExtractor.extract(path);
                default -> new ObjectNode(null);
            };
        }

        return new ObjectNode(null);
    }
}
