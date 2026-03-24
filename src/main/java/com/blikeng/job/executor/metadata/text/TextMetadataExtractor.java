package com.blikeng.job.executor.metadata.text;

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
    public static void extract(Path path, ObjectNode result) throws TikaException, IOException {
        Tika tika = new Tika();
        String content = tika.parseToString(path);

        result.put("category", "text");
        result.put("characters", content.length());
        result.put("words", content.trim().isEmpty() ? 0 : content.trim().split("\\s+").length);
        result.put("lines", content.isEmpty() ? 0 : content.split("\n").length);

        Metadata metadata = new Metadata();
        Parser parser = new AutoDetectParser();

        try (InputStream stream = Files.newInputStream(path)) {
            parser.parse(stream, new BodyContentHandler(), metadata, new ParseContext());
        } catch (Exception e) {
            throw new RuntimeException("Error parsing file", e);
        }

        for (String name : metadata.names()) {
            result.put(name, metadata.get(name));
        }
    }
}
