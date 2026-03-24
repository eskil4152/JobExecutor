package com.blikeng.job.executor.metadata.audio;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.AudioHeader;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;
import tools.jackson.databind.node.ObjectNode;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AudioMetadataExtractor {
    public static void extract(Path path, ObjectNode result) throws CannotReadException, TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        File file = path.toFile();
        result.put("category", "audio");
        result.put("fileSize", file.length());

        AudioFile audioFile = AudioFileIO.read(file);

        Tag tag = audioFile.getTag();
        if (tag != null) {
            result.put("artist", tag.getFirst(FieldKey.ARTIST));
            result.put("album", tag.getFirst(FieldKey.ALBUM));
            result.put("title", tag.getFirst(FieldKey.TITLE));
            result.put("comment", tag.getFirst(FieldKey.COMMENT));
            result.put("year", tag.getFirst(FieldKey.YEAR));
            result.put("track", tag.getFirst(FieldKey.TRACK));
            result.put("genre", tag.getFirst(FieldKey.GENRE));
            result.put("composer", tag.getFirst(FieldKey.COMPOSER));
            result.put("language", tag.getFirst(FieldKey.LANGUAGE));
            result.put("recordLabel", tag.getFirst(FieldKey.RECORD_LABEL));
            result.put("rating", tag.getFirst(FieldKey.RATING));
            result.put("barcode", tag.getFirst(FieldKey.BARCODE));
            result.put("music_brainz_id", tag.getFirst(FieldKey.MUSICIP_ID));
        }

        AudioHeader header = audioFile.getAudioHeader();
        if (header != null) {
            result.put("duration", header.getTrackLength());
            result.put("bitrate", header.getBitRate());
            result.put("vbt", header.isVariableBitRate());
            result.put("channels", header.getChannels());
            result.put("format", header.getFormat());
            result.put("sampleRate", header.getSampleRate());
            result.put("encoding", header.getEncodingType());
            result.put("lossless", header.isLossless());
        }
    }
}
