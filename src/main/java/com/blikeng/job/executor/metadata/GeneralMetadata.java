package com.blikeng.job.executor.metadata;

import com.blikeng.job.executor.exception.MetadataException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFileAttributes;

public class GeneralMetadata {

    private GeneralMetadata(){
        /* This utility class should not be instantiated */
    }

    public static void getGeneralData(Path path, ObjectNode current) {
        BasicFileAttributes attributes;
        PosixFileAttributes posixAttributes;

        try {
            attributes = Files.readAttributes(path, BasicFileAttributes.class);
            posixAttributes = Files.readAttributes(path, PosixFileAttributes.class);
        } catch (IOException exception) {
            throw new MetadataException(InternalMessages.FAILED_TO_READ_FILE_ATTRIBUTES.getMessage(), "GeneralMetadata.getGeneralData", exception);
        }

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
