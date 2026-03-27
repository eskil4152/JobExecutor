package com.blikeng.job.executor.unitTests.handlerTests;

import com.blikeng.job.executor.exception.InvalidPayloadException;
import com.blikeng.job.executor.handler.JobHandler;
import com.blikeng.job.executor.service.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class JobHandlerTests {
    // ==========================
    // Tests for JobHandler. Verifies:
    // - ADD_NUMBERS returns correct sum
    // - COUNT_WORDS returns correct word count
    // - Null and blank content returns 0 words
    // - Invalid payload throws InvalidPayloadException
    // ==========================

    @Mock private StorageService storageService;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private JobHandler jobHandler;

    @BeforeEach
    void setUp() {
        jobHandler = new JobHandler(objectMapper, storageService);
    }

    // ==========================
    // Handle AddNumbers
    // ==========================
    @Test
    void shouldReturnCorrectSum() {
        JsonNode result = jobHandler.handleAddNumbers("""
                {
                  "a": 3,
                  "b": 4
                }
                """);

        assertThat(result.get("sum").asInt()).isEqualTo(7);
    }

    @Test
    void shouldThrowInvalidPayloadForMissingAddNumbersFields() {
        assertThatThrownBy(() -> jobHandler.handleAddNumbers("""
                {
                  "a": "notanumber",
                  "b": 4
                }
                """))
                .isInstanceOf(InvalidPayloadException.class);
    }

    @Test
    void shouldThrowInvalidPayloadForMalformedAddNumbersJson() {
        assertThatThrownBy(() -> jobHandler.handleAddNumbers("not json"))
                .isInstanceOf(InvalidPayloadException.class);
    }

    // ==========================
    // Handle CountWords
    // ==========================
    @Test
    void shouldReturnCorrectWordCount() {
        JsonNode result = jobHandler.handleCountWords("""
                {
                  "content": "hello world foo"
                }
                """);

        assertThat(result.get("words").asInt()).isEqualTo(3);
    }

    @Test
    void shouldReturnZeroWordsForBlankContent() {
        JsonNode result = jobHandler.handleCountWords("""
                {
                  "content": "   "
                }
                """);

        assertThat(result.get("words").asInt()).isZero();
    }

    @Test
    void shouldReturnZeroWordsForNullContent() {
        JsonNode result = jobHandler.handleCountWords("""
                {
                  "content": null
                }
                """);

        assertThat(result.get("words").asInt()).isZero();
    }

    @Test
    void shouldThrowInvalidPayloadForMalformedCountWordsJson() {
        assertThatThrownBy(() -> jobHandler.handleCountWords("not json"))
                .isInstanceOf(InvalidPayloadException.class);
    }
}
