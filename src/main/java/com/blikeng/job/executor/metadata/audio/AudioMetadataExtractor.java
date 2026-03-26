package com.blikeng.job.executor.metadata.audio;

import com.blikeng.job.executor.exception.MetadataException;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.nio.file.Path;

public class AudioMetadataExtractor {
    public static void extract(Path path, ObjectNode result) {
        File file = path.toFile();
        result.put("category", "audio");
        result.put("fileSize", file.length());

        AudioFile audioFile;

        try {
            audioFile = AudioFileIO.read(file);
        } catch (Exception e) {
            throw new MetadataException("Failed to read audio file", "AudioMetadataExtractor.extract", e);
        }

        Tag tag = audioFile.getTag();
        if (tag != null) {
            putTagField(result, tag, "artist", FieldKey.ARTIST);
            putTagField(result, tag, "album", FieldKey.ALBUM);
            putTagField(result, tag, "title", FieldKey.TITLE);
            putTagField(result, tag, "comment", FieldKey.COMMENT);
            putTagField(result, tag, "year", FieldKey.YEAR);
            putTagField(result, tag, "track", FieldKey.TRACK);
            putTagField(result, tag, "genre", FieldKey.GENRE);
            putTagField(result, tag, "composer", FieldKey.COMPOSER);
            putTagField(result, tag, "language", FieldKey.LANGUAGE);
            putTagField(result, tag, "recordLabel", FieldKey.RECORD_LABEL);
            putTagField(result, tag, "rating", FieldKey.RATING);
            putTagField(result, tag, "barcode", FieldKey.BARCODE);
        }

        AudioHeader header = audioFile.getAudioHeader();
        if (header != null) {
            result.put("duration", header.getTrackLength());
            result.put("bitrate", header.getBitRate());
            result.put("vbr", header.isVariableBitRate());
            result.put("channels", header.getChannels());
            result.put("format", header.getFormat());
            result.put("sampleRate", header.getSampleRate());
            result.put("encoding", header.getEncodingType());
            result.put("lossless", header.isLossless());
        }
    }

    private static void putTagField(ObjectNode result, Tag tag, String key, FieldKey field) {
        try {
            result.put(key, tag.getFirst(field));
        } catch (UnsupportedOperationException ignored) {
            // Some tag types don't support all FieldKey values
        }
    }
}
