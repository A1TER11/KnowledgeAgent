package com.resume.agent.shared.store;

import com.resume.agent.shared.model.ChatMessage;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryChatMessageStore implements ChatMessageStore {

    private final CopyOnWriteArrayList<ChatMessage> messages = new CopyOnWriteArrayList<>();

    @Override
    public ChatMessage save(ChatMessage message) {
        messages.add(message);
        return message;
    }

    @Override
    public List<ChatMessage> findBySessionId(String sessionId) {
        return messages.stream()
                .filter(message -> message.sessionId().equals(sessionId))
                .sorted(Comparator.comparing(ChatMessage::createdAt))
                .toList();
    }
}
