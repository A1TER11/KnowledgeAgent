package com.resume.agent.memory;

import com.resume.agent.config.AgentProperties;
import com.resume.agent.shared.model.ChatMessage;
import com.resume.agent.shared.store.ChatMessageStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ShortTermMemoryService {

    private final ChatMessageStore chatMessageStore;
    private final AgentProperties properties;

    public ShortTermMemoryService(ChatMessageStore chatMessageStore, AgentProperties properties) {
        this.chatMessageStore = chatMessageStore;
        this.properties = properties;
    }

    public List<ChatMessage> recentContext(String sessionId) {
        List<ChatMessage> messages = chatMessageStore.findBySessionId(sessionId);
        int fromIndex = Math.max(0, messages.size() - properties.getMemory().getShortTermWindow());
        return messages.subList(fromIndex, messages.size());
    }
}
