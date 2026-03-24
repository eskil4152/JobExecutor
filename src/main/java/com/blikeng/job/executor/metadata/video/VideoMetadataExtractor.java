package com.blikeng.job.executor.metadata.video;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class VideoMetadataExtractor {
    private final static ObjectMapper objectMapper = new ObjectMapper();

    public static void extract(Path path, ObjectNode result) throws IOException, InterruptedException {
        result.put("category", "video");
        result.put("fileSize", path.toFile().length());

        ProcessBuilder processBuilder = extractVideoData(path);

        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());

        int code = process.waitFor();
        if (code != 0) {
            throw new RuntimeException("Error extracting video metadata");
        }

        JsonNode probeResult = objectMapper.readTree(output);

        JsonNode format = probeResult.get("format");
        JsonNode streams = probeResult.get("streams");

        if (format != null) {
            copyFields(format, result, FORMAT_FIELDS);
        }

        if (streams != null && streams.isArray()) {
            for (JsonNode stream : streams) {
                if (!stream.has("codec_type")) {
                    continue;
                }

                String type = stream.get("codec_type").asString();

                if ("video".equals(type)) {
                    copyFields(stream, result, VIDEO_FIELDS);
                } else if ("audio".equals(type)) {
                    copyFields(stream, result, AUDIO_FIELDS);
                }
            }
        }
    }

    private static ProcessBuilder extractVideoData(Path path){
        return new ProcessBuilder(
                "ffprobe",
                "-v", "quiet",
                "-print_format", "json",
                "-show_format",
                "-show_streams",
                path.toString()
        );
    }

    private static void copyFields(JsonNode source, ObjectNode result, Map<String, String> fields) {
        for (Map.Entry<String, String> entry : fields.entrySet()) {
            String sourceKey = entry.getKey();
            String resultKey = entry.getValue();

            if (!source.has(sourceKey) || source.get(sourceKey).isNull()) {
                continue;
            }

            JsonNode value = source.get(sourceKey);

            if (value.isInt()) {
                result.put(resultKey, value.asInt());
            } else if (value.isLong()) {
                result.put(resultKey, value.asLong());
            } else if (value.isFloat() || value.isDouble() || value.isBigDecimal()) {
                result.put(resultKey, value.asDouble());
            } else if (value.isBoolean()) {
                result.put(resultKey, value.asBoolean());
            } else {
                result.put(resultKey, value.asString());
            }
        }
    }

    private static final Map<String, String> FORMAT_FIELDS = Map.of(
            "duration", "duration",
            "bit_rate", "bitrate",
            "format_name", "format",
            "size", "size"
    );

    private static final Map<String, String> VIDEO_FIELDS = Map.of(
            "codec_name", "video_codec",
            "width", "width",
            "height", "height",
            "avg_frame_rate", "frame_rate",
            "bit_rate", "video_bitrate",
            "profile", "profile",
            "level", "level"
    );

    private static final Map<String, String> AUDIO_FIELDS = Map.of(
            "codec_name", "audio_codec",
            "sample_rate", "sample_rate",
            "channels", "channels",
            "bit_rate", "audio_bitrate"
    );
}
