package com.blikeng.job.executor.exception.messages;

public enum InternalMessages {
    ALGORITHM_NOT_FOUND("Algorithm not found"),
    INVALID_DECRYPTION_PARAMS("Ciphertext, IV and key must not be null"),
    INVALID_COMPRESSED_TEXT("Compressed text must not be null"),
    DECRYPTION_FAILED("Decryption failed"),
    DECRYPTION_FAILED_AUTH_ERROR("Decryption failed: authentication error (wrong key/iv/data)"),
    ENCRYPTION_FAILED("Encryption failed"),
    FAILED_TO_COMPRESS("Failed compression"),
    FAILED_TO_DECOMPRESS("Failed decompression"),
    DECOMPRESSION_SIZE_LIMIT_EXCEEDED("Decompression size limit exceeded"),
    FAILED_TO_DETECT_FILE_TYPE("Failed to detect file type"),
    FAILED_TO_PARSE_TEXT_METADATA("Failed to parse text metadata"),
    FAILED_TO_READ_AUDIO_FILE("Failed to read audio file"),
    FAILED_TO_READ_FILE("Failed to read file"),
    FAILED_TO_READ_FILE_ATTRIBUTES("Failed to read file attributes"),
    FAILED_TO_READ_IMAGE_METADATA("Failed to read image metadata"),
    FAILED_TO_READ_VIDEO_METADATA("Failed to read video metadata"),
    INVALID_FILE_CRYPTO_PARAMS("File ID, IV and key must not be null"),
    FILE_NOT_FOUND("File not found"),
    FILE_READ_WRITE_FAILED("File read/write failed"),
    INVALID_HASH_INPUTS("Hash inputs must not be null or blank"),
    INVALID_AES_KEY_LENGTH("Invalid AES key length"),
    INVALID_BASE64("Invalid Base64"),
    INVALID_KEY_OR_PARAMETERS("Invalid key or parameters"),
    INVALID_PAYLOAD("Invalid payload"),
    INVALID_TEXT_CONTENT("Invalid text content"),
    INVALID_ZIP_ENTRY_PATH("Invalid zip entry path"),
    METADATA_EXTRACTION_INTERRUPTED("Metadata extraction interrupted"),
    ZIP_ENTRY_IS_DIRECTORY("Zip entry is a directory"),
    ZIP_FILE_CONTAINS_MULTIPLE_ENTRIES("Zip file contains multiple entries"),
    ZIP_IS_EMPTY("Zip is empty");

    private final String message;

    InternalMessages(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return message;
    }
}
