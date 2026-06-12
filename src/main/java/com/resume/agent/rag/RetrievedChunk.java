package com.resume.agent.rag;

public record RetrievedChunk(
        String documentId,
        String title,
        String content,
        double score
) {
}
