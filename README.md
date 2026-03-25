# Job Executor

Job Executor is an asynchronous job processing service built with Java and Spring Boot.
Clients submit jobs via a REST API and receive a job ID immediately. Jobs are executed in a background thread pool and results are persisted in PostgreSQL, ready to be polled at any time.

---

## Table of Contents

- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Features](#features)
  - [Job Lifecycle](#job-lifecycle)
  - [File Storage](#file-storage)
  - [Hashing](#hashing)
  - [Compression](#compression)
  - [Encryption](#encryption)
  - [Metadata Extraction](#metadata-extraction)
  - [File Analysis](#file-analysis)
  - [Error Handling](#error-handling)
- [API Overview](#api-overview)
- [Job Types & Payloads](#job-types--payloads)

---

## Tech Stack

| Layer               | Technology                             |
|---------------------|----------------------------------------|
| Language            | Java 24                                |
| Framework           | Spring Boot 4.0.4                      |
| Database            | PostgreSQL (schema managed via Flyway) |
| ORM                 | Spring Data JPA / Hibernate            |
| File Type Detection | Apache Tika 3.3.0                      |
| Image Metadata      | metadata-extractor 2.19.0              |
| Audio Metadata      | jaudiotagger 3.0.1                     |
| Observability       | Spring Boot Actuator                   |

---

## Architecture

```
POST /api/job
     │
     ▼
JobController → JobService → JobRepository (persist, status: QUEUED)
                           → WorkerManager.submitTask(JobTask)
                                    │
                              fixed thread pool (2 workers)
                                    │
                                    ▼
                           JobExecutionService.execute()
                                    │
                             switch on JobType
                                    │
                    ┌───────────────┼────────────────┐
                    ▼               ▼                ▼
              JobHandler    HashHandler    CompressionHandler    ...
                                    │
                           JobRepository (persist result, status: COMPLETED/FAILED)

GET /api/job/{id}  →  JobRepository (read status + result)
```

Jobs are accepted immediately and processed asynchronously. The submitting thread is never blocked by job execution.

---

## Features

### Job Lifecycle

- On submission, a job is persisted to PostgreSQL with status `QUEUED` and a UUID is returned to the caller.
- The job is immediately dispatched to a fixed-size thread pool of **2 workers**.
- When a worker picks up the job, status transitions to `RUNNING` and a start timestamp is recorded.
- On completion, the result is serialized to JSON and saved alongside the job. Status becomes `COMPLETED`.
- On failure, a descriptive error message is saved as the result and status becomes `FAILED`.
- Clients poll `GET /api/job/{id}` to check status and retrieve results.

---

### File Storage

- Files are uploaded via `POST /api/file/` as multipart form data.
- Each uploaded file is assigned a UUID-prefixed filename and stored in the `./uploads/` directory.
- The returned file ID is then passed as `fileId` in payloads for any file-based job.
- Uploaded files can be downloaded at any time via `GET /api/file/{fileId}`.
- The upload limit is set in `application.properties`.

---

### Hashing

- Files and text strings can be hashed using any algorithm supported by `java.security.MessageDigest`.
- The algorithm is specified per job. Defaults to **SHA-256** if not provided.
- Common, supported algorithms: `MD5`, `SHA-1`, `SHA-256`, `SHA-512`.
- A dedicated comparison job accepts two hash strings and returns whether they match.

---

### Compression

- Files can be compressed to ZIP format and decompressed back. The output is saved alongside the original file in `./uploads/`.
- Text strings can be compressed to a ZIP and returned as a **Base64-encoded** string, and decompressed back from Base64.
- File decompression enforces single-entry ZIPs only — archives with multiple entries are rejected.
- A path traversal check is applied during decompression to ensure extracted files cannot escape the storage directory.

---

### Encryption

Encryption is **AES-256-GCM** (AES/GCM/NoPadding with a 128-bit authentication tag).

**Keys**
- Keys must be **Base64-encoded** and decode to exactly 16, 24, or 32 bytes (AES-128, AES-192, or AES-256).
- If no key is provided on encryption, a random 256-bit key is generated and returned in the result.

**IV**
- The IV is always randomly generated (12 bytes via `SecureRandom`) and included in the encryption result.
- For decryption, the same `iv` returned by the encryption job must be supplied.

**File encryption**
- Encrypted files are saved as `<original>.enc` in `./uploads/`.
- Decrypted files are saved as `<original>.decrypted`.

**Text encryption**
- Plaintext is encrypted and returned as a Base64-encoded ciphertext string.
- Decryption accepts the Base64 ciphertext and returns the original plaintext.

---

### Metadata Extraction

Metadata extraction is powered by **Apache Tika** for file type detection, with type-specific extractors for each category:

| File Type   | Metadata Extracted                                                                                                          |
|-------------|-----------------------------------------------------------------------------------------------------------------------------|
| All files   | Name, size, timestamps (created, modified, accessed), owner, group                                                         |
| Image       | Dimensions (JPEG, PNG, GIF, BMP, WebP), camera make/model/software, orientation, exposure time, f-number, ISO, focal length, color space, resolution, GPS coordinates |
| Audio       | ID3 tags (artist, album, title, track, year, genre, composer, language, record label, rating, barcode) + header (duration, bitrate, sample rate, channels, format, encoding, lossless) |
| Video       | Duration, bitrate, format, video codec, width, height, frame rate, audio codec, sample rate, channels — extracted via `ffprobe` (must be installed) |
| Text        | Character count, word count, line count, and all Tika-parsed metadata fields                                                |
| Application | File type category only — format-specific extraction (PDF, DOCX, ZIP, etc.) is not yet implemented                         |

---

### File Analysis

- Reads a plain text file and returns word count, line count, character count, and byte size.

---

### Error Handling

There are two distinct layers of error handling:

**HTTP layer — `ApiException` / `GlobalExceptionHandler`**
- `ApiException` is thrown by the controllers and `JobService` for client-facing errors (invalid UUID, job not found, bad request payload, etc.).
- `GlobalExceptionHandler` catches `ApiException` and returns the appropriate HTTP status and message to the client.

**Job execution layer — internal exceptions**
- Exceptions thrown inside handlers during job execution are caught by `JobExecutionService`, which marks the job as `FAILED` and logs the error. Clients are never exposed to the raw exception — polling the job will show `FAILED` status with a descriptive message indicating the cause (e.g. invalid payload, algorithm not found, file processing failure).
- Each internal exception carries a `location` field (class and method name) for precise log tracing.

| Exception                | Cause                                              |
|--------------------------|----------------------------------------------------|
| `InvalidPayloadException`| Missing or malformed job payload fields            |
| `FileProcessingException`| I/O failure during file read, write, or extraction |
| `MetadataException`      | Failure during metadata extraction                 |
| `AlgorithmException`     | Unsupported or invalid cryptographic algorithm     |
| `JobException`           | Job not found or unsupported job type              |

---

## API Overview

| Method | Endpoint           | Description               |
|--------|--------------------|---------------------------|
| POST   | /api/job           | Submit a job              |
| GET    | /api/job/{id}      | Get job status and result |
| POST   | /api/file/         | Upload a file             |
| GET    | /api/file/{fileId} | Download a file           |

**Actuator**

| Method | Endpoint          | Description                    |
|--------|-------------------|--------------------------------|
| GET    | /actuator/health  | Application health             |
| GET    | /actuator/info    | App name, description, version |
| GET    | /actuator/metrics | JVM and application metrics    |

---

## Job Types & Payloads

All jobs are submitted to `POST /api/job` with the body:
```json
{
  "jobType": "<JOB_TYPE>",
  "payload": { ... }
}
```
The response is a UUID string. Poll `GET /api/job/{id}` for status and result.

---

### `ADD_NUMBERS`
Adds two integers.

Payload:
```json
{ "a": 10, "b": 32 }
```
Result:
```json
{ "sum": 42 }
```

---

### `COUNT_WORDS`
Counts whitespace-separated words in a string.

Payload:
```json
{ "content": "Hello world" }
```
Result:
```json
{ "words": 2 }
```

---

### `ANALYZE_FILE`
Counts words, lines, characters, and bytes in a text file.

Payload:
```json
{ "fileId": "<fileId>" }
```
Result:
```json
{ "words": 42, "lines": 10, "characters": 250, "bytes": 260 }
```

---

### `EXTRACT_METADATA`
Extracts general and type-specific metadata from a file.

Payload:
```json
{ "fileId": "<fileId>" }
```
Result varies by file type. Always includes general attributes; additional fields depend on the detected file type.

---

### `HASH_FILE`
Hashes a file. `algorithm` is optional, defaults to `SHA-256`.

Payload:
```json
{ "fileId": "<fileId>", "algorithm": "SHA-256" }
```
Result:
```json
{ "hash": "<hex>", "algorithm": "SHA-256" }
```

---

### `HASH_TEXT`
Hashes a string. `algorithm` is optional, defaults to `SHA-256`.

Payload:
```json
{ "content": "hello world", "algorithm": "SHA-512" }
```
Result:
```json
{ "hash": "<hex>", "algorithm": "SHA-512" }
```

---

### `COMPARE_HASHES`
Compares two hash strings for equality.

Payload:
```json
{ "hashA": "<hex>", "hashB": "<hex>" }
```
Result:
```json
{ "match": true }
```

---

### `COMPRESS_FILE`
ZIP-compresses an uploaded file. Output is saved alongside the original.

Payload:
```json
{ "fileId": "<fileId>" }
```
Result:
```json
{ "file_path": "<filename>.zip" }
```

---

### `DECOMPRESS_FILE`
Decompresses a single-entry ZIP file. Output is saved alongside the ZIP.

Payload:
```json
{ "fileId": "<fileId>" }
```
Result:
```json
{ "decompressed_file": "<filename>" }
```

---

### `COMPRESS_TEXT`
ZIP-compresses a string and returns the result as Base64.

Payload:
```json
{ "content": "Hello, world!" }
```
Result:
```json
{ "compressed_text": "<base64>" }
```

---

### `DECOMPRESS_TEXT`
Decompresses a Base64-encoded ZIP string.

Payload:
```json
{ "content": "<base64>" }
```
Result:
```json
{ "text": "Hello, world!" }
```

---

### `ENCRYPT_TEXT`
Encrypts a plaintext string with AES-256-GCM. `key` is optional — omit to have one generated.

Payload:
```json
{ "content": "secret message", "key": "<base64-key>" }
```
Result:
```json
{
  "algorithm": "AES/GCM/NoPadding",
  "cipherText": "<base64>",
  "iv": "<base64>",
  "key": "<base64>"
}
```

---

### `DECRYPT_TEXT`
Decrypts a Base64-encoded AES-GCM ciphertext.

Payload:
```json
{ "content": "<base64-ciphertext>", "key": "<base64-key>", "iv": "<base64-iv>" }
```
Result:
```json
{ "content": "secret message" }
```

---

### `ENCRYPT_FILE`
Encrypts an uploaded file. Output is saved as `<original>.enc`. `key` is optional.

Payload:
```json
{ "fileId": "<fileId>", "key": "<base64-key>" }
```
Result:
```json
{
  "outputFile": "<path>",
  "algorithm": "AES/GCM/NoPadding",
  "iv": "<base64>",
  "key": "<base64>"
}
```

---

### `DECRYPT_FILE`
Decrypts an encrypted file. Output is saved as `<original>.decrypted`.

Payload:
```json
{ "fileId": "<fileId>", "key": "<base64-key>", "iv": "<base64-iv>" }
```
Result:
```json
{ "file_path": "<filename>.decrypted" }
```
