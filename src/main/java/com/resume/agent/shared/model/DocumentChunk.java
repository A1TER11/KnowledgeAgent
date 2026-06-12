package com.resume.agent.shared.model;

import java.util.List;
import java.util.UUID;

public record DocumentChunk(
        String chunkId,
        String documentId,
        String title,
        String content,
        List<Double> embedding
) {
    public static DocumentChunk create(String documentId, String title, String content, List<Double> embedding) {
        return new DocumentChunk(UUID.randomUUID().toString(), documentId, title, content, embedding);
    }
}
