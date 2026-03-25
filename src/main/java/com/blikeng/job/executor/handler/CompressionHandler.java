package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
public class CompressionHandler extends BaseHandler {
    protected CompressionHandler(ObjectMapper objectMapper, StorageService storageService) {
        super(objectMapper, storageService);
    }

    public JsonNode handleFileCompression(String payloadString) {
        FilePayload payload = parsePayload(payloadString, FilePayload.class, "File Compression");

        try {
            Path path = getFilePath(payload.fileId(), "File Compression");
            Path zipPath = path.resolveSibling(path.getFileName().toString() + ".zip");

            try (
                    InputStream in = Files.newInputStream(path);
                    ZipOutputStream zipOut = new ZipOutputStream(Files.newOutputStream(zipPath))
            ) {
                ZipEntry zipEntry = new ZipEntry(path.getFileName().toString());
                zipOut.putNextEntry(zipEntry);

                copy(in, zipOut, "File Compression");

                zipOut.closeEntry();
            }

            return objectMapper.createObjectNode()
                    .put("file_path", zipPath.getFileName().toString());

        } catch (IOException e) {
            throw new FileProcessingException("Unable to compress file for File Compression", e);
        }
    }

    public JsonNode handleFileDecompression(String payloadString) {
        FilePayload payload = parsePayload(payloadString, FilePayload.class, "File Decompression");
        Path zipPath = getFilePath(payload.fileId(), "File Decompression");

        try (
                ZipInputStream zipIn = new ZipInputStream(Files.newInputStream(zipPath));
        ) {
            ZipEntry zipEntry = zipIn.getNextEntry();
            Path outputPath = resolveSafeZipOutputPath(zipPath, zipEntry);

            try (OutputStream out = Files.newOutputStream(outputPath)) {
                copy(zipIn, out, "File Decompression");
            }

            zipIn.closeEntry();

            if (zipIn.getNextEntry() != null) throw new FileProcessingException("Zip file contains multiple entries", null);

            return objectMapper.createObjectNode()
                    .put("decompressed_file", outputPath.getFileName().toString());

        } catch (IOException e) {
            throw new FileProcessingException("Unable to decompress file for File Decompression", e);
        }
    }

    private Path resolveSafeZipOutputPath(Path zipPath, ZipEntry zipEntry) {
        if (zipEntry == null) {
            throw new FileProcessingException("Zip file is empty", null);
        }

        if (zipEntry.isDirectory()) {
            throw new FileProcessingException("Zip entry is a directory", null);
        }

        Path outputPath = zipPath.resolveSibling(zipEntry.getName()).normalize();
        Path parent = outputPath.getParent();

        if (parent == null || !parent.equals(zipPath.getParent())) {
            throw new FileProcessingException("Invalid zip entry path", null);
        }

        return outputPath;
    }

    private void copy(InputStream in, OutputStream out, String context) {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new FileProcessingException("I/O failure during " + context, e);
        }
    }
}
