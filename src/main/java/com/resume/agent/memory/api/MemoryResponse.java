package com.resume.agent.memory.api;

import java.time.Instant;
import java.util.List;

public record MemoryResponse(
        String userId,
        List<MemoryItemView> memories
) {
    public record MemoryItemView(String memoryId, String memoryType, String content, String source, Instant createdAt) {
    }
}
