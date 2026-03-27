package com.blikeng.job.executor.payloadTests;

import com.blikeng.job.executor.payloads.EncryptionResultPayload;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EncryptionResultPayloadTests {
    @Test
    void shouldBeEqualWhenByteArrayContentsMatch() {
        byte[] a1 = new byte[]{1, 2, 3};
        byte[] a2 = new byte[]{1, 2, 3};

        EncryptionResultPayload left = new EncryptionResultPayload("AES", a1, "iv", "key");
        EncryptionResultPayload right = new EncryptionResultPayload("AES", a2, "iv", "key");

        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void shouldIncludeRelevantFieldsInToString() {
        EncryptionResultPayload payload =
                new EncryptionResultPayload("AES", new byte[]{1, 2, 3}, "iv", "key");

        assertTrue(payload.toString().contains("AES"));
    }
}
