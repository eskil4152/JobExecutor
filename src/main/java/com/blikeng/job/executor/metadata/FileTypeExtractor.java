package com.blikeng.job.executor.metadata;

import com.blikeng.job.executor.exception.MetadataException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import com.blikeng.job.executor.metadata.application.ApplicationMetadataExtractor;
import com.blikeng.job.executor.metadata.audio.AudioMetadataExtractor;
import com.blikeng.job.executor.metadata.image.ImageMetadataExtractor;
import com.blikeng.job.executor.metadata.text.TextMetadataExtractor;
import com.blikeng.job.executor.metadata.video.VideoMetadataExtractor;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;

public class FileTypeExtractor {

    private FileTypeExtractor(){
        /* This utility class should not be instantiated */
    }

    private final static Logger logger = LoggerFactory.getLogger(FileTypeExtractor.class);

    public static void findFileType(Path path, ObjectNode result) {
        Tika tika = new Tika();
        String contentType;

        try {
             contentType = tika.detect(path);
        } catch (IOException e) {
            throw new MetadataException(InternalMessages.FAILED_TO_DETECT_FILE_TYPE.getMessage(), "FileTypeExtractor.findFileType", e);
        }

        String[] parts = contentType.split("/");
        String fileType = parts[0];

        switch (fileType) {
            case "text" -> TextMetadataExtractor.extract(path, result);
            case "application" -> ApplicationMetadataExtractor.extract(result);
            case "image" -> ImageMetadataExtractor.extract(path, result);
            case "video" -> VideoMetadataExtractor.extract(path, result);
            case "audio" -> AudioMetadataExtractor.extract(path, result);
            default -> logger.warn("Unsupported file type: {}", fileType);
        }
    }
}
