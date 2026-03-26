package com.blikeng.job.executor.service;

import com.blikeng.job.executor.exception.http.ApiException;
import com.blikeng.job.executor.exception.messages.ApiMessages;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;

@Service
public class StorageService {
    private final Path storagePath;

    public StorageService(@Value("${storage.path:./uploads}") String storagePath) throws IOException {
        this.storagePath = Paths.get(storagePath).toAbsolutePath().normalize();
        Files.createDirectories(this.storagePath);
    }

    public String store(MultipartFile file) throws IOException {
        String original = file.getOriginalFilename();
        String safeName = (original == null || original.isBlank())
                ? "file"
                : Path.of(original).getFileName().toString();

        safeName = safeName.replaceAll("[^a-zA-Z0-9._-]", "_");

        String filename = UUID.randomUUID() + "_" + safeName;
        Path destination = storagePath.resolve(filename).normalize();

        Files.copy(file.getInputStream(), destination);

        return filename;
    }

    public Resource getFile(String fileId) {
        try {
            Path path = getPath(fileId).normalize();

            if (!path.startsWith(storagePath)) {
                throw new ApiException(ApiMessages.URL_NOT_VALID.getMessage(), HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new FileNotFoundException();
            }

            return resource;

        } catch (MalformedURLException _) {
            throw new ApiException(ApiMessages.URL_NOT_VALID.getMessage(), HttpStatus.BAD_REQUEST);
        } catch (FileNotFoundException _) {
            throw new ApiException(ApiMessages.FILE_NOT_FOUND.getMessage(), HttpStatus.NOT_FOUND);
        }
    }

    public Path getPath(String id){
        return storagePath.resolve(id);
    }
}
