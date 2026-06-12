package com.resume.agent.shared.store;

import com.resume.agent.shared.model.ChatMessage;
import java.util.List;

public interface ChatMessageStore {
    ChatMessage save(ChatMessage message);

    List<ChatMessage> findBySessionId(String sessionId);
}
