package com.resume.agent.shared.store.jdbc;

import com.resume.agent.shared.model.LongTermMemory;
import com.resume.agent.shared.model.MemoryType;
import com.resume.agent.shared.store.MemoryStore;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcMemoryStore implements MemoryStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcMemoryStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public LongTermMemory save(LongTermMemory memory) {
        jdbcTemplate.update("""
                        INSERT INTO user_memory (memory_id, user_id, memory_type, content, source, embedding, created_at)
                        VALUES (?, ?, ?, ?, ?, CAST(? AS vector), ?)
                        """,
                new Object[]{
                        memory.memoryId(),
                        memory.userId(),
                        memory.memoryType().name(),
                        memory.content(),
                        memory.source(),
                        JdbcVectorSupport.toVectorLiteral(memory.embedding()),
                        Timestamp.from(memory.createdAt())
                },
                new int[]{
                        Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.TIMESTAMP
                });
        return memory;
    }

    @Override
    public List<LongTermMemory> findByUserId(String userId) {
        return jdbcTemplate.query("""
                        SELECT memory_id, user_id, memory_type, content, source, embedding::text AS embedding, created_at
                        FROM user_memory
                        WHERE user_id = ?
                        ORDER BY created_at DESC
                        """,
                (rs, rowNum) -> new LongTermMemory(
                        rs.getString("memory_id"),
                        rs.getString("user_id"),
                        MemoryType.valueOf(rs.getString("memory_type")),
                        rs.getString("content"),
                        rs.getString("source"),
                        JdbcVectorSupport.parseVector(rs.getString("embedding")),
                        rs.getTimestamp("created_at").toInstant()),
                userId);
    }

    @Override
    public List<LongTermMemory> searchByEmbedding(String userId, List<Double> embedding, int topK, double minScore) {
        String vectorLiteral = JdbcVectorSupport.toVectorLiteral(embedding);
        return jdbcTemplate.query("""
                        SELECT memory_id, user_id, memory_type, content, source, embedding::text AS embedding, created_at
                        FROM (
                            SELECT *,
                                   1 - (embedding <=> CAST(? AS vector)) AS score
                            FROM user_memory
                            WHERE user_id = ?
                        ) ranked
                        WHERE score >= ?
                        ORDER BY score DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> new LongTermMemory(
                        rs.getString("memory_id"),
                        rs.getString("user_id"),
                        MemoryType.valueOf(rs.getString("memory_type")),
                        rs.getString("content"),
                        rs.getString("source"),
                        JdbcVectorSupport.parseVector(rs.getString("embedding")),
                        rs.getTimestamp("created_at").toInstant()),
                vectorLiteral,
                userId,
                minScore,
                topK);
    }
}
