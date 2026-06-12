package com.resume.agent.docs.api;

import java.util.List;

public record DocumentDetailView(
        String documentId,
        String title,
        String rawContent,
        int chunkCount,
        List<DocumentChunkView> chunks
) {
    public record DocumentChunkView(
            String chunkId,
            String title,
            String content
    ) {
    }
}
