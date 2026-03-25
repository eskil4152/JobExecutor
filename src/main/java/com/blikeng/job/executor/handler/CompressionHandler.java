package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.payloads.FilePayload;
import com.blikeng.job.executor.payloads.TextPayload;
import com.blikeng.job.executor.service.StorageService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
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

    public JsonNode handleTextCompression(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "Text Compression");

        if (payload.content() == null || payload.content().isBlank()) {
            throw new FileProcessingException("Text content must not be null or empty", null);
        }

        try (
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ZipOutputStream zipOut = new ZipOutputStream(out);
        ){
            ZipEntry entry = new ZipEntry("text.txt");
            zipOut.putNextEntry(entry);

            byte[] data = payload.content().getBytes(StandardCharsets.UTF_8);
            zipOut.write(data, 0, data.length);
            zipOut.closeEntry();
            zipOut.finish();

            String base = Base64.getEncoder().encodeToString(out.toByteArray());

            return objectMapper.createObjectNode()
                    .put("compressed_text", base);

        } catch (IOException e) {
            throw new FileProcessingException("Unable to compress text for Text Compression", e);
        }
    }

    public JsonNode handleTextDecompression(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "Text Decompression");

        if (payload.content() == null) {
            throw new InvalidPayloadException("Compressed text must not be null", null);
        }

        byte[] compressed;

        try {
            compressed = Base64.getDecoder().decode(payload.content());
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException("Invalid base64 encoded text", e);
        }

        try (
            ByteArrayInputStream byteIn = new ByteArrayInputStream(compressed);
            ZipInputStream zipIn = new ZipInputStream(byteIn)
        ) {
            ZipEntry entry = zipIn.getNextEntry();
            if (entry == null) {
                throw new FileProcessingException("Zip is empty", null);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(zipIn, out, "Text Decompression");

            zipIn.closeEntry();

            String result = out.toString(StandardCharsets.UTF_8);

            return objectMapper.createObjectNode()
                    .put("text", result);

        } catch (IOException e) {
            throw new FileProcessingException("Unable to decompress text", e);
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
