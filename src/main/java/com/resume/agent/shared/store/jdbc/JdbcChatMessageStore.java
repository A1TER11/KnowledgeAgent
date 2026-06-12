package com.resume.agent.shared.store.jdbc;

import com.resume.agent.shared.model.ChatMessage;
import com.resume.agent.shared.model.MessageRole;
import com.resume.agent.shared.store.ChatMessageStore;
import java.sql.Timestamp;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcChatMessageStore implements ChatMessageStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcChatMessageStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public ChatMessage save(ChatMessage message) {
        jdbcTemplate.update("""
                        INSERT INTO chat_message (id, session_id, user_id, role, content, created_at)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """,
                message.id(),
                message.sessionId(),
                message.userId(),
                message.role().name(),
                message.content(),
                Timestamp.from(message.createdAt()));
        return message;
    }

    @Override
    public List<ChatMessage> findBySessionId(String sessionId) {
        return jdbcTemplate.query("""
                        SELECT id, session_id, user_id, role, content, created_at
                        FROM chat_message
                        WHERE session_id = ?
                        ORDER BY created_at ASC
                        """,
                (rs, rowNum) -> new ChatMessage(
                        rs.getString("id"),
                        rs.getString("session_id"),
                        rs.getString("user_id"),
                        MessageRole.valueOf(rs.getString("role")),
                        rs.getString("content"),
                        rs.getTimestamp("created_at").toInstant()),
                sessionId);
    }
}
