package com.blikeng.job.executor.controller;

import com.blikeng.job.executor.service.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/file")
public class FileUploadController {
    private final StorageService storageService;

    public FileUploadController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String id = storageService.store(file);

        return ResponseEntity.ok().body("File uploaded: " + id);
    }
}
