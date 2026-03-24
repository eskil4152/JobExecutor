package com.blikeng.job.executor.metadata.image;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;

public class ImageMetadataExtractor {
    public static void extract(Path path, ObjectNode result) throws ImageProcessingException, IOException {
        Metadata metadata = ImageMetadataReader.readMetadata(path.toFile());

        result.put("category", "image");

        for (Directory dir : metadata.getDirectories()) {
            for (Tag tag : dir.getTags()) {
                String rawKey = dir.getName() + "." + tag.getTagName();

                if (!ImageMetadataWhitelist.ALLOWED.contains(rawKey)) {
                    continue;
                }

                String key = normalizeKey(rawKey);
                String value = tag.getDescription();

                if (value == null) {
                    result.putNull(key);
                } else {
                    result.put(key, value);
                }
            }
        }
    }

    private static String normalizeKey(String key){
        return key.toLowerCase().replace(" ", "_");
    }
}
