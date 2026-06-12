package com.resume.agent.agent;

import com.resume.agent.memory.LongTermMemoryService;
import com.resume.agent.memory.ShortTermMemoryService;
import com.resume.agent.rag.RagService;
import com.resume.agent.shared.model.ChatMessage;
import com.resume.agent.shared.model.MemoryType;
import com.resume.agent.shared.model.MessageRole;
import com.resume.agent.shared.store.ChatMessageStore;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final ChatMessageStore chatMessageStore;
    private final ShortTermMemoryService shortTermMemoryService;
    private final LongTermMemoryService longTermMemoryService;
    private final RagService ragService;

    public AgentOrchestrator(
            ChatMessageStore chatMessageStore,
            ShortTermMemoryService shortTermMemoryService,
            LongTermMemoryService longTermMemoryService,
            RagService ragService
    ) {
        this.chatMessageStore = chatMessageStore;
        this.shortTermMemoryService = shortTermMemoryService;
        this.longTermMemoryService = longTermMemoryService;
        this.ragService = ragService;
    }

    public AgentDecision prepare(String sessionId, String userId, String message) {
        chatMessageStore.save(ChatMessage.create(sessionId, userId, MessageRole.USER, message));

        List<ChatMessage> recentMessages = shortTermMemoryService.recentContext(sessionId);
        List<com.resume.agent.rag.RetrievedChunk> knowledgeHits = safeKnowledgeSearch(message);
        List<com.resume.agent.memory.MemorySearchResult> memoryHits = safeMemorySearch(userId, message);

        extractMemoryFromMessage(userId, message);
        return new AgentDecision(sessionId, userId, message, recentMessages, knowledgeHits, memoryHits, List.of());
    }

    public void recordAssistantAnswer(String sessionId, String userId, String answer) {
        chatMessageStore.save(ChatMessage.create(sessionId, userId, MessageRole.ASSISTANT, answer));
    }

    private List<com.resume.agent.rag.RetrievedChunk> safeKnowledgeSearch(String message) {
        try {
            return ragService.search(message);
        } catch (Exception exception) {
            log.warn("Knowledge search failed, continuing without knowledge hits: {}", exception.getMessage());
            return List.of();
        }
    }

    private List<com.resume.agent.memory.MemorySearchResult> safeMemorySearch(String userId, String message) {
        try {
            return longTermMemoryService.searchRelevant(userId, message, 3);
        } catch (Exception exception) {
            log.warn("Memory search failed, continuing without memory hits: {}", exception.getMessage());
            return List.of();
        }
    }

    private void extractMemoryFromMessage(String userId, String message) {
        if (message == null || message.length() > 240 || message.contains("\n") || message.contains("##")) {
            return;
        }
        if (message.contains("喜欢") || message.contains("偏好")) {
            longTermMemoryService.remember(userId, MemoryType.USER_PREFERENCE, message, "chat");
            return;
        }
        if (message.contains("已经完成") || message.contains("已完成")) {
            longTermMemoryService.remember(userId, MemoryType.TASK_RESULT, message, "chat");
            return;
        }
        if (message.contains("负责") || message.contains("项目背景") || message.contains("公司规定")) {
            longTermMemoryService.remember(userId, MemoryType.BUSINESS_FACT, message, "chat");
        }
    }
}
