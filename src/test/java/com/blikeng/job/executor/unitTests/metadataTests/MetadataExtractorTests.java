package com.blikeng.job.executor.unitTests.metadataTests;

import com.blikeng.job.executor.exception.MetadataException;
import com.blikeng.job.executor.metadata.FileTypeExtractor;
import com.blikeng.job.executor.metadata.GeneralMetadata;
import com.blikeng.job.executor.metadata.audio.AudioMetadataExtractor;
import com.blikeng.job.executor.metadata.image.ImageMetadataExtractor;
import com.blikeng.job.executor.metadata.text.TextMetadataExtractor;
import com.blikeng.job.executor.metadata.video.VideoMetadataExtractor;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Base64;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class MetadataExtractorTests {
    // ==========================
    // Tests for metadata extractors. Verifies:
    // - GeneralMetadata reads file attributes
    // - GeneralMetadata throws MetadataException for missing file
    // - FileTypeExtractor routes to correct extractor by MIME type
    // - TextMetadataExtractor extracts character/line/word counts
    // - TextMetadataExtractor throws MetadataException for missing file
    // - ImageMetadataExtractor extracts category and whitelist tags
    // - ImageMetadataExtractor throws MetadataException for missing file
    // - FileTypeExtractor routes to audio extractor for WAV file
    // - FileTypeExtractor routes to video extractor for real MP4 file
    // - AudioMetadataExtractor extracts header fields from minimal WAV
    // - AudioMetadataExtractor extracts tag fields when present
    // - AudioMetadataExtractor handles missing tag fields
    // - AudioMetadataExtractor throws MetadataException for invalid audio
    // - VideoMetadataExtractor throws MetadataException on bad ffprobe path
    // - VideoMetadataExtractor throws MetadataException when ffprobe exits non-zero
    // - VideoMetadataExtractor handles successful probe output, missing streams,
    //   non-array streams, and interrupted extraction
    // - VideoMetadataExtractor parse/copy logic handles format/audio/video fields,
    //   missing blocks, null values, missing fields, and multiple value types
    // Note: contentType != null false branch and parts.length == 0 branch are dead code
    //       (Tika never returns null; String.split never returns empty array)
    // Note: AudioMetadataExtractor header != null false branch is dead code
    //       (jaudiotagger always sets AudioHeader on a successfully read file)
    // ==========================

    private static final byte[] MINIMAL_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
    );

    private static final byte[] MINIMAL_WAV = buildWav();
    private static final byte[] TAGGED_WAV = buildWavWithTags();

    @TempDir
    Path tempDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ObjectNode node() {
        return objectMapper.createObjectNode();
    }

    private Path copyResourceToTemp(String resourceName, String targetName) throws Exception {
        Path source = Path.of("src/test/resources", resourceName);
        Path target = tempDir.resolve(targetName);
        Files.copy(source, target);
        return target;
    }

    private Path writeFakeProbe(String fileName, String body) throws Exception {
        Path script = tempDir.resolve(fileName);
        Files.writeString(script, "#!/bin/sh\n" + body + "\n");
        Files.setPosixFilePermissions(script, Set.of(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_EXECUTE
        ));
        return script;
    }

    private static byte[] buildWav() {
        int sampleRate = 8000;
        int numSamples = 8000;
        int bitsPerSample = 8;
        int numChannels = 1;
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;
        int dataSize = numSamples * blockAlign;

        ByteBuffer buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[]{'R', 'I', 'F', 'F'});
        buf.putInt(36 + dataSize);
        buf.put(new byte[]{'W', 'A', 'V', 'E'});
        buf.put(new byte[]{'f', 'm', 't', ' '});
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) numChannels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) bitsPerSample);
        buf.put(new byte[]{'d', 'a', 't', 'a'});
        buf.putInt(dataSize);
        return buf.array();
    }

    private static byte[] buildWavWithTags() {
        int sampleRate = 8000;
        int numSamples = 8000;
        int bitsPerSample = 8;
        int numChannels = 1;
        int byteRate = sampleRate * numChannels * bitsPerSample / 8;
        int blockAlign = numChannels * bitsPerSample / 8;
        int dataSize = numSamples * blockAlign;

        byte[] artistData = new byte[]{'T', 'e', 's', 't', 'A', 'r', 't', 'i', 's', 't', 0, 0};
        int artistActualSize = 11;
        int listContentSize = 4 + 4 + 4 + artistData.length;
        int totalSize = 12 + 24 + 8 + listContentSize + 8 + dataSize;

        ByteBuffer buf = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);
        buf.put(new byte[]{'R', 'I', 'F', 'F'});
        buf.putInt(totalSize - 8);
        buf.put(new byte[]{'W', 'A', 'V', 'E'});
        buf.put(new byte[]{'f', 'm', 't', ' '});
        buf.putInt(16);
        buf.putShort((short) 1);
        buf.putShort((short) numChannels);
        buf.putInt(sampleRate);
        buf.putInt(byteRate);
        buf.putShort((short) blockAlign);
        buf.putShort((short) bitsPerSample);
        buf.put(new byte[]{'L', 'I', 'S', 'T'});
        buf.putInt(listContentSize);
        buf.put(new byte[]{'I', 'N', 'F', 'O'});
        buf.put(new byte[]{'I', 'A', 'R', 'T'});
        buf.putInt(artistActualSize);
        buf.put(artistData);
        buf.put(new byte[]{'d', 'a', 't', 'a'});
        buf.putInt(dataSize);
        return buf.array();
    }

    // ==========================
    // GeneralMetadata
    // ==========================
    @Test
    void shouldReadGeneralFileAttributes() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello");

        ObjectNode result = node();
        GeneralMetadata.getGeneralData(file, result);

        assertThat(result.get("name").asString()).isEqualTo("test.txt");
        assertThat(result.get("size").asLong()).isEqualTo(Files.size(file));
        assertThat(result.get("owner")).isNotNull();
    }

    @Test
    void shouldThrowMetadataExceptionForMissingFileOnGeneralMetadata() {
        Path nonExistent = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> GeneralMetadata.getGeneralData(nonExistent, node()))
                .isInstanceOf(MetadataException.class);
    }

    // ==========================
    // FileTypeExtractor
    // ==========================
    @Test
    void shouldRouteToTextExtractorForTxtFile() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world");

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category").asString()).isEqualTo("text");
    }

    @Test
    void shouldRouteToImageExtractorForPngFile() throws Exception {
        Path file = tempDir.resolve("test.png");
        Files.write(file, MINIMAL_PNG);

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category").asString()).isEqualTo("image");
    }

    @Test
    void shouldRouteToApplicationExtractorForBinaryFile() throws Exception {
        Path file = tempDir.resolve("test.bin");
        Files.write(file, new byte[]{0x00, 0x01, 0x02, 0x03});

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category").asString()).isEqualTo("application");
    }

    @Test
    void shouldHitDefaultBranchForUnsupportedMimeType() throws Exception {
        Path file = tempDir.resolve("test.eml");
        Files.writeString(file, """
                From: sender@example.com
                To: recipient@example.com
                Subject: Test
                Date: Thursday
                MIME-Version: 1.0

                This is the body.
                """);

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category")).isNull();
    }

    @Test
    void shouldThrowMetadataExceptionForMissingFileOnTypeExtractor() {
        Path nonExistent = tempDir.resolve("missing.bin");

        assertThatThrownBy(() -> FileTypeExtractor.findFileType(nonExistent, node()))
                .isInstanceOf(MetadataException.class);
    }

    @Test
    void shouldRouteToAudioExtractorForWavFile() throws Exception {
        Path file = tempDir.resolve("test.wav");
        Files.write(file, MINIMAL_WAV);

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category").asString()).isEqualTo("audio");
    }

    @Test
    void shouldRouteToVideoExtractorForMp4File() throws Exception {
        Path file = copyResourceToTemp("video/tiny.mp4", "test.mp4");

        ObjectNode result = node();
        FileTypeExtractor.findFileType(file, result);

        assertThat(result.get("category").asString()).isEqualTo("video");
    }

    // ==========================
    // TextMetadataExtractor
    // ==========================
    @Test
    void shouldExtractTextMetadata() throws Exception {
        Path file = tempDir.resolve("test.txt");
        Files.writeString(file, "hello world\nfoo bar");

        ObjectNode result = node();
        TextMetadataExtractor.extract(file, result);

        assertThat(result.get("category").asString()).isEqualTo("text");
        assertThat(result.get("characters").asInt()).isGreaterThan(0);
        assertThat(result.get("lines").asInt()).isEqualTo(2);
    }

    @Test
    void shouldThrowMetadataExceptionForMissingTextFile() {
        Path nonExistent = tempDir.resolve("missing.txt");

        assertThatThrownBy(() -> TextMetadataExtractor.extract(nonExistent, node()))
                .isInstanceOf(MetadataException.class);
    }

    // ==========================
    // ImageMetadataExtractor
    // ==========================
    @Test
    void shouldExtractImageMetadata() throws Exception {
        Path file = tempDir.resolve("test.png");
        Files.write(file, MINIMAL_PNG);

        ObjectNode result = node();
        ImageMetadataExtractor.extract(file, result);

        assertThat(result.get("category").asString()).isEqualTo("image");
        assertThat(result.get("png-ihdr.image_width").asString()).isEqualTo("1");
        assertThat(result.get("png-ihdr.image_height").asString()).isEqualTo("1");
    }

    @Test
    void shouldThrowMetadataExceptionForMissingImageFile() {
        Path nonExistent = tempDir.resolve("missing.png");

        assertThatThrownBy(() -> ImageMetadataExtractor.extract(nonExistent, node()))
                .isInstanceOf(MetadataException.class);
    }

    // ==========================
    // AudioMetadataExtractor
    // ==========================
    @Test
    void shouldExtractAudioMetadataFromWav() throws Exception {
        Path file = tempDir.resolve("test.wav");
        Files.write(file, MINIMAL_WAV);

        ObjectNode result = node();
        AudioMetadataExtractor.extract(file, result);

        assertThat(result.get("category").asString()).isEqualTo("audio");
        assertThat(result.get("fileSize").asLong()).isEqualTo(MINIMAL_WAV.length);
        assertThat(result.get("format").asString()).isNotBlank();
        assertThat(result.get("sampleRate").asString()).isEqualTo("8000");
        assertThat(result.get("channels").asString()).isEqualTo("1");
    }

    @Test
    void shouldExtractAudioTagFieldsWhenTagIsPresent() throws Exception {
        Path file = tempDir.resolve("tagged.wav");
        Files.write(file, TAGGED_WAV);

        ObjectNode result = node();
        AudioMetadataExtractor.extract(file, result);

        assertThat(result.get("category").asString()).isEqualTo("audio");
        assertThat(result.get("artist").asString()).startsWith("TestArtist");
    }

    @Test
    void shouldThrowMetadataExceptionForInvalidAudioFile() throws Exception {
        Path file = tempDir.resolve("invalid.mp3");
        Files.write(file, "not audio data".getBytes());

        assertThatThrownBy(() -> AudioMetadataExtractor.extract(file, node()))
                .isInstanceOf(MetadataException.class);
    }

    @Test
    void shouldHandleMissingAudioTagFields() throws Exception {
        Path file = tempDir.resolve("untagged.wav");
        Files.write(file, MINIMAL_WAV);

        ObjectNode result = node();
        AudioMetadataExtractor.extract(file, result);

        assertThat(result.get("category").asString()).isEqualTo("audio");
        assertThat(result.get("format")).isNotNull();
        assertThat(result.get("artist").asString()).isEmpty();
        assertThat(result.get("album").asString()).isEmpty();
        assertThat(result.get("title").asString()).isEmpty();
    }

    // ==========================
    // VideoMetadataExtractor
    // ==========================
    // ==========================
    // VideoMetadataExtractor
    // ==========================
    @Test
    void shouldThrowMetadataExceptionWhenFfprobeFails() throws Exception {
        Path file = tempDir.resolve("invalid.mp4");
        Files.write(file, "not a video".getBytes());

        assertThatThrownBy(() -> VideoMetadataExtractor.extract(file, node()))
                .isInstanceOf(MetadataException.class);
    }

    @Test
    void shouldThrowMetadataExceptionOnIoException() throws Exception {
        Path file = tempDir.resolve("dummy.mp4");
        Files.writeString(file, "data");

        assertThatThrownBy(() ->
                VideoMetadataExtractor.extract(file, node(), "/nonexistent/path/ffprobe"))
                .isInstanceOf(MetadataException.class);
    }

    @Test
    void shouldThrowMetadataExceptionWhenFfprobeExitNonZero() throws Exception {
        Path fakeProbe = writeFakeProbe("ffprobe-exit-1.sh", "exit 1");
        Path file = tempDir.resolve("dummy.mp4");
        Files.writeString(file, "data");

        assertThatThrownBy(() ->
                VideoMetadataExtractor.extract(file, node(), fakeProbe.toString()))
                .isInstanceOf(MetadataException.class);
    }

    @Test
    void shouldExtractVideoMetadataWhenProbeSucceeds() throws Exception {
        Path video = tempDir.resolve("test.mp4");
        Files.write(video, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-success.sh",
                "echo '{\"format\":{\"duration\":\"12.5\",\"bit_rate\":\"1000\",\"format_name\":\"mp4\",\"size\":\"3\"},\"streams\":[{\"codec_type\":\"video\",\"codec_name\":\"h264\",\"width\":1920,\"height\":1080,\"avg_frame_rate\":\"30/1\",\"bit_rate\":\"800\",\"profile\":\"High\",\"level\":40},{\"codec_type\":\"audio\",\"codec_name\":\"aac\",\"sample_rate\":\"48000\",\"channels\":2,\"bit_rate\":\"200\"}]}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(video, result, script.toAbsolutePath().toString());

        assertThat(result.get("category").asString()).isEqualTo("video");
        assertThat(result.get("fileSize").asLong()).isEqualTo(3);
        assertThat(result.get("duration").asString()).isEqualTo("12.5");
        assertThat(result.get("bitrate").asString()).isEqualTo("1000");
        assertThat(result.get("format").asString()).isEqualTo("mp4");
        assertThat(result.get("size").asString()).isEqualTo("3");
        assertThat(result.get("video_codec").asString()).isEqualTo("h264");
        assertThat(result.get("width").asInt()).isEqualTo(1920);
        assertThat(result.get("height").asInt()).isEqualTo(1080);
        assertThat(result.get("frame_rate").asString()).isEqualTo("30/1");
        assertThat(result.get("video_bitrate").asString()).isEqualTo("800");
        assertThat(result.get("profile").asString()).isEqualTo("High");
        assertThat(result.get("level").asInt()).isEqualTo(40);
        assertThat(result.get("audio_codec").asString()).isEqualTo("aac");
        assertThat(result.get("sample_rate").asString()).isEqualTo("48000");
        assertThat(result.get("channels").asInt()).isEqualTo(2);
        assertThat(result.get("audio_bitrate").asString()).isEqualTo("200");
    }

    @Test
    void shouldThrowMetadataExceptionWhenInterrupted() throws Exception {
        Path video = tempDir.resolve("test.mp4");
        Files.write(video, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "sleepy-ffprobe.sh",
                "sleep 5\necho '{\"format\":{\"format_name\":\"mp4\"}}'"
        );

        ObjectNode result = node();
        AtomicReference<Throwable> thrown = new AtomicReference<>();

        Thread thread = new Thread(() -> {
            try {
                VideoMetadataExtractor.extract(video, result, script.toAbsolutePath().toString());
            } catch (Throwable t) {
                thrown.set(t);
            }
        });

        thread.start();
        Thread.sleep(200);
        thread.interrupt();
        thread.join();

        assertThat(thrown.get()).isInstanceOf(MetadataException.class)
                .hasMessageContaining("Metadata extraction interrupted");
    }

    @Test
    void shouldHandleMissingStreamsInSuccessfulProbe() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-no-streams.sh",
                "echo '{\"format\":{\"duration\":\"42.0\",\"bit_rate\":\"999\",\"format_name\":\"mp4\",\"size\":\"123\"}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("category").asString()).isEqualTo("video");
        assertThat(result.get("duration").asString()).isEqualTo("42.0");
        assertThat(result.get("bitrate").asString()).isEqualTo("999");
        assertThat(result.get("format").asString()).isEqualTo("mp4");
        assertThat(result.get("size").asString()).isEqualTo("123");
        assertThat(result.get("video_codec")).isNull();
        assertThat(result.get("audio_codec")).isNull();
    }

    @Test
    void shouldIgnoreStreamsWhenStreamsIsNotArray() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-object-streams.sh",
                "echo '{\"format\":{\"format_name\":\"mp4\"},\"streams\":{\"codec_type\":\"video\",\"codec_name\":\"h264\"}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("category").asString()).isEqualTo("video");
        assertThat(result.get("format").asString()).isEqualTo("mp4");
        assertThat(result.get("video_codec")).isNull();
        assertThat(result.get("audio_codec")).isNull();
    }

    @Test
    void shouldSkipStreamWithoutCodecType() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-missing-codec-type.sh",
                "echo '{\"streams\":[{\"codec_name\":\"h264\",\"width\":1920}]}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("video_codec")).isNull();
        assertThat(result.get("width")).isNull();
    }

    @Test
    void shouldSkipStreamWithUnknownCodecType() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-unknown-codec-type.sh",
                "echo '{\"streams\":[{\"codec_type\":\"subtitle\",\"codec_name\":\"srt\"}]}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("video_codec")).isNull();
        assertThat(result.get("audio_codec")).isNull();
    }

    @Test
    void shouldSkipFormatBlockWhenFormatNodeAbsent() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-no-format.sh",
                "echo '{\"streams\":[{\"codec_type\":\"video\",\"codec_name\":\"h264\"}]}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("duration")).isNull();
        assertThat(result.get("format")).isNull();
        assertThat(result.get("video_codec").asString()).isEqualTo("h264");
    }

    @Test
    void shouldSkipNullFieldValues() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-null-fields.sh",
                "echo '{\"format\":{\"duration\":null,\"format_name\":\"mp4\"}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("duration")).isNull();
        assertThat(result.get("format").asString()).isEqualTo("mp4");
    }

    @Test
    void shouldSkipMissingFields() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-missing-fields.sh",
                "echo '{\"format\":{\"format_name\":\"webm\"}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("duration")).isNull();
        assertThat(result.get("bitrate")).isNull();
        assertThat(result.get("size")).isNull();
        assertThat(result.get("format").asString()).isEqualTo("webm");
    }

    @Test
    void shouldCopyLongFieldValue() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-long.sh",
                "echo '{\"format\":{\"bit_rate\":9876543210}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("bitrate").asLong()).isEqualTo(9876543210L);
    }

    @Test
    void shouldCopyDoubleFieldValue() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-double.sh",
                "echo '{\"format\":{\"duration\":1.5}}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("duration").asDouble()).isEqualTo(1.5);
    }

    @Test
    void shouldCopyBooleanFieldValue() throws Exception {
        Path file = tempDir.resolve("test.mp4");
        Files.write(file, new byte[]{1, 2, 3});

        Path script = writeFakeProbe(
                "fake-ffprobe-boolean.sh",
                "echo '{\"streams\":[{\"codec_type\":\"video\",\"level\":true}]}'"
        );

        ObjectNode result = node();
        VideoMetadataExtractor.extract(file, result, script.toAbsolutePath().toString());

        assertThat(result.get("level").asBoolean()).isTrue();
    }
}