package com.resume.agent.config;

import com.resume.agent.docs.api.DocumentDetailView;
import com.resume.agent.docs.api.DocumentUploadRequest;
import com.resume.agent.llm.EmbeddingClient;
import com.resume.agent.rag.RagService;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
public class EmbeddingMaintenanceRunner {

    private static final Logger log = LoggerFactory.getLogger(EmbeddingMaintenanceRunner.class);
    private static final int HNSW_MAX_DIMENSIONS = 2000;

    private final JdbcTemplate jdbcTemplate;
    private final EmbeddingClient embeddingClient;
    private final RagService ragService;

    public EmbeddingMaintenanceRunner(
            JdbcTemplate jdbcTemplate,
            EmbeddingClient embeddingClient,
            RagService ragService
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.embeddingClient = embeddingClient;
        this.ragService = ragService;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        try {
            int embeddingDimensions = embeddingClient.embed("dimension probe").size();
            if (embeddingDimensions <= 0) {
                log.warn("Embedding maintenance skipped because the embedding dimension could not be determined.");
                return;
            }

            migrateVectorColumns(embeddingDimensions);
            rebuildKnowledgeChunks();
        } catch (Exception exception) {
            log.warn("Embedding maintenance skipped: {}", exception.getMessage());
        }
    }

    private void migrateVectorColumns(int dimensions) {
        // Old 12-dim demo vectors are incompatible with the real embedding dimensions,
        // so we rebuild document chunks from raw_content and clear outdated long-term memories.
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_document_chunk_embedding");
        jdbcTemplate.execute("DROP INDEX IF EXISTS idx_user_memory_embedding");
        jdbcTemplate.execute("DELETE FROM document_chunk");
       jdbcTemplate.execute("DELETE FROM user_memory");
        jdbcTemplate.execute("ALTER TABLE document_chunk ALTER COLUMN embedding TYPE vector(" + dimensions + ")");
        jdbcTemplate.execute("ALTER TABLE user_memory ALTER COLUMN embedding TYPE vector(" + dimensions + ")");
        if (dimensions <= HNSW_MAX_DIMENSIONS) {
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
                    ON document_chunk USING hnsw (embedding vector_cosine_ops)
                    """);
            jdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_user_memory_embedding
                    ON user_memory USING hnsw (embedding vector_cosine_ops)
                    """);
        } else {
            log.warn("Skip hnsw index creation because embedding dimensions {} exceed pgvector hnsw limit {}.",
                    dimensions, HNSW_MAX_DIMENSIONS);
        }
        log.info("Embedding columns migrated to {} dimensions.", dimensions);
    }

    private void rebuildKnowledgeChunks() {
        List<String> documentIds = ragService.listDocuments().stream()
                .map(document -> document.documentId())
                .toList();

        for (String documentId : documentIds) {
            DocumentDetailView detail = ragService.documentDetail(documentId);
            ragService.update(documentId, new DocumentUploadRequest(detail.title(), detail.rawContent()));
        }

        log.info("Rebuilt embeddings for {} knowledge documents.", documentIds.size());
    }
}
