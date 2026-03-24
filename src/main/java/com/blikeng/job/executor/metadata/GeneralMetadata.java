package com.blikeng.job.executor.metadata;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;

public class GeneralMetadata {
    public static void getGeneralData(Path path, ObjectNode current) throws IOException {
        BasicFileAttributes attributes = Files.readAttributes(path, BasicFileAttributes.class);
        PosixFileAttributes posixAttributes = Files.readAttributes(path, PosixFileAttributes.class);

        current.put("name", path.getFileName().toString());
        current.put("size", attributes.size());
        current.put("lastModified", attributes.lastModifiedTime().toMillis());
        current.put("created", attributes.creationTime().toMillis());
        current.put("accessed", attributes.lastAccessTime().toMillis());
        current.put("isSymbolicLink", attributes.isSymbolicLink());

        current.put("owner", posixAttributes.owner().getName());
        current.put("group", posixAttributes.group().getName());
    }
}
