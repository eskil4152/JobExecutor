package com.blikeng.job.executor.service;

import com.blikeng.job.executor.exception.Http.ApiException;
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
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path destination = storagePath.resolve(filename);

        Files.copy(file.getInputStream(), destination);

        return filename;
    }

    public Resource getFile(String fileId) {
        try {
            Path path = getPath(fileId).normalize();

            if (!path.startsWith(storagePath)) {
                throw new ApiException("URL not valid", HttpStatus.BAD_REQUEST);
            }

            Resource resource = new UrlResource(path.toUri());

            if (!resource.exists()) {
                throw new FileNotFoundException();
            }

            return resource;

        } catch (MalformedURLException e) {
            throw new ApiException("URL not valid", HttpStatus.BAD_REQUEST);
        } catch (FileNotFoundException e) {
            throw new ApiException("File not found", HttpStatus.NOT_FOUND);
        }
    }

    public Path getPath(String id){
        return storagePath.resolve(id);
    }
}
