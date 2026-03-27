package com.blikeng.job.executor.exceptionTests;

import com.blikeng.job.executor.exception.messages.InternalMessages;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InternalMessagesTests {
    @Test
    void shouldReturnMessageInToString() {
        assertEquals("Invalid payload", InternalMessages.INVALID_PAYLOAD.toString());
    }

    @Test
    void shouldReturnMessageInGetMessage() {
        assertEquals("Invalid payload", InternalMessages.INVALID_PAYLOAD.getMessage());
    }
}
