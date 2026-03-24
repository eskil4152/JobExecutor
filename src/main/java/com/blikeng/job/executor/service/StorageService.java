package com.blikeng.job.executor.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {
    private final Path storagePath = Paths.get("./uploads");

    public StorageService() throws IOException {
        Files.createDirectories(storagePath);
    }

    public String store(MultipartFile file) throws IOException {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = storagePath.resolve(filename);

        Files.copy(file.getInputStream(), destination);

        return filename;
    }

    public Path getPath(String id){
        try {
            return storagePath.resolve(id);
        } catch (Exception e) {
            return null;
        }
    }
}
