package com.resume.agent.agent;

import com.resume.agent.memory.MemorySearchResult;
import com.resume.agent.rag.RetrievedChunk;
import com.resume.agent.shared.model.ChatMessage;
import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.List;

public record AgentDecision(
        String sessionId,
        String userId,
        String userMessage,
        List<ChatMessage> recentMessages,
        List<RetrievedChunk> knowledgeHits,
        List<MemorySearchResult> memoryHits,
        List<ToolExecutionRecord> toolExecutions
) {
}
