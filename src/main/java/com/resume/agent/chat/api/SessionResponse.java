package com.resume.agent.chat.api;

import java.time.Instant;
import java.util.List;

public record SessionResponse(
        String sessionId,
        List<MessageView> messages
) {
    public record MessageView(String role, String content, Instant createdAt) {
    }
}
