package com.resume.agent.shared.model;

import java.util.List;
import java.util.UUID;

public record KnowledgeDocument(
        String documentId,
        String title,
        String rawContent,
        List<DocumentChunk> chunks
) {
    public static KnowledgeDocument create(String title, String rawContent, List<DocumentChunk> chunks) {
        return new KnowledgeDocument(UUID.randomUUID().toString(), title, rawContent, chunks);
    }
}
