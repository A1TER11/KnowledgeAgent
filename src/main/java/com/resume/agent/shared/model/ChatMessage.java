package com.resume.agent.shared.model;

import java.time.Instant;
import java.util.UUID;

public record ChatMessage(
        String id,
        String sessionId,
        String userId,
        MessageRole role,
        String content,
        Instant createdAt
) {
    public static ChatMessage create(String sessionId, String userId, MessageRole role, String content) {
        return new ChatMessage(UUID.randomUUID().toString(), sessionId, userId, role, content, Instant.now());
    }
}
