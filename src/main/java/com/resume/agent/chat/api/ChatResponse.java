package com.resume.agent.chat.api;

import java.util.List;

public record ChatResponse(
        String sessionId,
        String userId,
        String answer,
        List<ContextSnippetView> knowledgeSnippets,
        List<MemoryView> longTermMemories,
        List<ToolCallView> toolCalls
) {
    public record ContextSnippetView(String documentId, String title, String content, double score) {
    }

    public record MemoryView(String memoryId, String memoryType, String content, double score) {
    }

    public record ToolCallView(String toolName, String summary, String details) {
    }
}
