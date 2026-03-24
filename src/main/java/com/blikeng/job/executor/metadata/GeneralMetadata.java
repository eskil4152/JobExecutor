package com.blikeng.job.executor.metadata;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;

public class GeneralMetadata {
    public static final ObjectMapper objectMapper = new ObjectMapper();

    public static ObjectNode getGeneralData(Path path) throws IOException {
        ObjectNode result = objectMapper.createObjectNode();

        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        PosixFileAttributes posixAttributes = Files.readAttributes(path, PosixFileAttributes.class);

        result.put("name", path.getFileName().toString());
        result.put("size", attributes.size());
        result.put("lastModified", attributes.lastModifiedTime().toMillis());
        result.put("created", attributes.creationTime().toMillis());
        result.put("accessed", attributes.lastAccessTime().toMillis());
        result.put("isSymbolicLink", attributes.isSymbolicLink());

        result.put("owner", posixAttributes.owner().getName());
        result.put("group", posixAttributes.group().getName());

        return result;
    }
}
