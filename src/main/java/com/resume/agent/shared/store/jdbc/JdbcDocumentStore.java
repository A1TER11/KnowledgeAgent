package com.resume.agent.shared.store.jdbc;

import com.resume.agent.shared.model.DocumentChunk;
import com.resume.agent.shared.model.KnowledgeDocument;
import com.resume.agent.shared.store.DocumentStore;
import java.sql.Types;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;

public class JdbcDocumentStore implements DocumentStore {

    private final JdbcTemplate jdbcTemplate;

    public JdbcDocumentStore(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        jdbcTemplate.update("""
                        INSERT INTO knowledge_document (document_id, title, raw_content)
                        VALUES (?, ?, ?)
                        ON CONFLICT (document_id) DO UPDATE
                        SET title = EXCLUDED.title, raw_content = EXCLUDED.raw_content
                        """,
                document.documentId(),
                document.title(),
                document.rawContent());

        jdbcTemplate.update("DELETE FROM document_chunk WHERE document_id = ?", document.documentId());
        for (DocumentChunk chunk : document.chunks()) {
            jdbcTemplate.update("""
                            INSERT INTO document_chunk (chunk_id, document_id, title, content, embedding)
                            VALUES (?, ?, ?, ?, CAST(? AS vector))
                            """,
                    new Object[]{
                            chunk.chunkId(),
                            chunk.documentId(),
                            chunk.title(),
                            chunk.content(),
                            JdbcVectorSupport.toVectorLiteral(chunk.embedding())
                    },
                    new int[]{Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR, Types.VARCHAR});
        }
        return document;
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return jdbcTemplate.query("""
                        SELECT document_id, title, raw_content
                        FROM knowledge_document
                        ORDER BY title ASC
                        """,
                (rs, rowNum) -> new KnowledgeDocument(
                        rs.getString("document_id"),
                        rs.getString("title"),
                        rs.getString("raw_content"),
                        findChunks(rs.getString("document_id"))));
    }

    @Override
    public Optional<KnowledgeDocument> findById(String documentId) {
        List<KnowledgeDocument> documents = jdbcTemplate.query("""
                        SELECT document_id, title, raw_content
                        FROM knowledge_document
                        WHERE document_id = ?
                        """,
                (rs, rowNum) -> new KnowledgeDocument(
                        rs.getString("document_id"),
                        rs.getString("title"),
                        rs.getString("raw_content"),
                        findChunks(rs.getString("document_id"))),
                documentId);
        return documents.stream().findFirst();
    }

    @Override
    public List<KnowledgeDocument> searchByEmbedding(List<Double> embedding, int topK, double minScore) {
        String vectorLiteral = JdbcVectorSupport.toVectorLiteral(embedding);
        return jdbcTemplate.query("""
                        SELECT DISTINCT document_id
                        FROM (
                            SELECT document_id,
                                   1 - (embedding <=> CAST(? AS vector)) AS score
                            FROM document_chunk
                        ) ranked
                        WHERE score >= ?
                        ORDER BY score DESC
                        LIMIT ?
                        """,
                (rs, rowNum) -> findById(rs.getString("document_id")).orElseThrow(),
                vectorLiteral,
                minScore,
                topK);
    }

    @Override
    public void deleteById(String documentId) {
        jdbcTemplate.update("DELETE FROM knowledge_document WHERE document_id = ?", documentId);
    }

    @Override
    public boolean existsByTitle(String title) {
        Integer count = jdbcTemplate.queryForObject("""
                        SELECT COUNT(1)
                        FROM knowledge_document
                        WHERE title = ?
                        """,
                Integer.class,
                title);
        return count != null && count > 0;
    }

    private List<DocumentChunk> findChunks(String documentId) {
        return jdbcTemplate.query("""
                        SELECT chunk_id, document_id, title, content, embedding::text AS embedding
                        FROM document_chunk
                        WHERE document_id = ?
                        """,
                (rs, rowNum) -> new DocumentChunk(
                        rs.getString("chunk_id"),
                        rs.getString("document_id"),
                        rs.getString("title"),
                        rs.getString("content"),
                        JdbcVectorSupport.parseVector(rs.getString("embedding"))),
                documentId);
    }
}
