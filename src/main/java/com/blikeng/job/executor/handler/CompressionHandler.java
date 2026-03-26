package com.blikeng.job.executor.handler;

import com.blikeng.job.executor.exception.FileProcessingException;
import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.exception.messages.InternalMessages;
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
    public CompressionHandler(ObjectMapper objectMapper, StorageService storageService) {
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

                copy(in, zipOut);

                zipOut.closeEntry();
            }

            return objectMapper.createObjectNode()
                    .put("file_path", zipPath.getFileName().toString());

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_COMPRESS.getMessage(), "CompressionHandler.handleFileCompression", e);
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
                copy(zipIn, out);
            }

            zipIn.closeEntry();

            if (zipIn.getNextEntry() != null) throw new FileProcessingException(InternalMessages.ZIP_FILE_CONTAINS_MULTIPLE_ENTRIES.getMessage(), "CompressionHandler.handleFileDeompression", null);

            return objectMapper.createObjectNode()
                    .put("decompressed_file", outputPath.getFileName().toString());

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_DECOMPRESS.getMessage(), "CompressionHandler.handleFileDeompression", e);
        }
    }

    public JsonNode handleTextCompression(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "Text Compression");

        if (payload.content() == null || payload.content().isBlank()) {
            throw new FileProcessingException(InternalMessages.INVALID_TEXT_CONTENT.getMessage(), "CompressionHandler.handleTextCompression", null);
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
            throw new FileProcessingException(InternalMessages.FAILED_TO_COMPRESS.getMessage(), "CompressionHandler.handleTextCompression", e);
        }
    }

    public JsonNode handleTextDecompression(String payloadString) {
        TextPayload payload = parsePayload(payloadString, TextPayload.class, "Text Decompression");

        if (payload.content() == null) {
            throw new InvalidPayloadException(InternalMessages.INVALID_COMPRESSED_TEXT.getMessage(), "CompressionHandler.handleTextDecompression", null);
        }

        byte[] compressed;

        try {
            compressed = Base64.getDecoder().decode(payload.content());
        } catch (IllegalArgumentException e) {
            throw new InvalidPayloadException(InternalMessages.INVALID_BASE64.getMessage(), "CompressionHandler.handleTextDecompression", e);
        }

        try (
            ByteArrayInputStream byteIn = new ByteArrayInputStream(compressed);
            ZipInputStream zipIn = new ZipInputStream(byteIn)
        ) {
            ZipEntry entry = zipIn.getNextEntry();
            if (entry == null) {
                throw new FileProcessingException(InternalMessages.ZIP_IS_EMPTY.getMessage(), "CompressionHandler.handleTextDecompression", null);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            copy(zipIn, out);

            zipIn.closeEntry();

            String result = out.toString(StandardCharsets.UTF_8);

            return objectMapper.createObjectNode()
                    .put("text", result);

        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FAILED_TO_DECOMPRESS.getMessage(), "CompressionHandler.handleTextDecompression", e);
        }
    }

    private Path resolveSafeZipOutputPath(Path zipPath, ZipEntry zipEntry) {
        if (zipEntry == null) {
            throw new FileProcessingException(
                    InternalMessages.ZIP_IS_EMPTY.getMessage(),
                    "CompressionHandler.resolveSafeZipOutputPath",
                    null
            );
        }

        if (zipEntry.isDirectory()) {
            throw new FileProcessingException(
                    InternalMessages.ZIP_ENTRY_IS_DIRECTORY.getMessage(),
                    "CompressionHandler.resolveSafeZipOutputPath",
                    null
            );
        }

        Path targetDir = zipPath.getParent();
        if (targetDir == null) {
            throw new FileProcessingException(
                    InternalMessages.INVALID_ZIP_ENTRY_PATH.getMessage(),
                    "CompressionHandler.resolveSafeZipOutputPath",
                    null
            );
        }

        Path outputPath = targetDir.resolve(zipEntry.getName()).normalize();

        if (!outputPath.startsWith(targetDir)) {
            throw new FileProcessingException(
                    InternalMessages.INVALID_ZIP_ENTRY_PATH.getMessage(),
                    "CompressionHandler.resolveSafeZipOutputPath",
                    null
            );
        }

        if (outputPath.equals(targetDir)) {
            throw new FileProcessingException(
                    InternalMessages.INVALID_ZIP_ENTRY_PATH.getMessage(),
                    "CompressionHandler.resolveSafeZipOutputPath",
                    null
            );
        }

        return outputPath;
    }

    private void copy(InputStream in, OutputStream out) {
        try {
            byte[] buffer = new byte[1024];
            int length;
            while ((length = in.read(buffer)) != -1) {
                out.write(buffer, 0, length);
            }
        } catch (IOException e) {
            throw new FileProcessingException(InternalMessages.FILE_READ_WRITE_FAILED.getMessage(), "CompressionHandler.copy", e);
        }
    }
}
