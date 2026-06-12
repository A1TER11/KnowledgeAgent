package com.resume.agent.memory;

import com.resume.agent.shared.model.MemoryType;

public record MemorySearchResult(
        String memoryId,
        MemoryType memoryType,
        String content,
        double score
) {
}
