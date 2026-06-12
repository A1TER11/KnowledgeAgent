package com.resume.agent.docs.api;

public record DocumentView(
        String documentId,
        String title,
        int chunkCount
) {
}
