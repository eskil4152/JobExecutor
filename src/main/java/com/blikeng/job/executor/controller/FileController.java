package com.blikeng.job.executor.controller;

import com.blikeng.job.executor.service.StorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/file")
public class FileController {
    private final StorageService storageService;

    public FileController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping()
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file
    ) throws IOException {
        String id = storageService.store(file);

        return ResponseEntity.ok().body("File uploaded: " + id);
    }

    @GetMapping("/{fileId}")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileId) {
        Resource resource = storageService.getFile(fileId);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(resource);
    }
}
