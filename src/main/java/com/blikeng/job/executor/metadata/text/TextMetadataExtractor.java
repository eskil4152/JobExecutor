package com.blikeng.job.executor.metadata.text;

import com.blikeng.job.executor.exception.MetadataException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import org.apache.tika.Tika;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.sax.BodyContentHandler;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

public class TextMetadataExtractor {

    private TextMetadataExtractor(){
        /* This utility class should not be instantiated */
    }

    public static void extract(Path path, ObjectNode result) {
        Tika tika = new Tika();
        String content;

        try {
             content = tika.parseToString(path);
        } catch (IOException | TikaException e) {
            throw new MetadataException(InternalMessages.FAILED_TO_READ_FILE.getMessage(), "TextMetadataExtractor.extract", e);
        }

        result.put("category", "text");
        result.put("characters", content.length());
        result.put("content", content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length);
        result.put("lines", content.isEmpty() ? 0 : content.split("\n").length);

        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();

        try (InputStream stream = Files.newInputStream(path)) {
            parser.parse(stream, new BodyContentHandler(), metadata, new ParseContext());
        } catch (Exception e) {
            throw new MetadataException(InternalMessages.FAILED_TO_PARSE_TEXT_METADATA.getMessage(), "TextMetadataExtractor.extract", e);
        }

        for (String name : metadata.names()) {
            result.put(name, metadata.get(name));
        }
    }
}
