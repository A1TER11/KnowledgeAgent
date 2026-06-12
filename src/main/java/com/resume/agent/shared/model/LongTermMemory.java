package com.resume.agent.shared.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record LongTermMemory(
        String memoryId,
        String userId,
        MemoryType memoryType,
        String content,
        String source,
        List<Double> embedding,
        Instant createdAt
) {
    public static LongTermMemory create(String userId, MemoryType memoryType, String content, String source, List<Double> embedding) {
        return new LongTermMemory(UUID.randomUUID().toString(), userId, memoryType, content, source, embedding, Instant.now());
    }
}
