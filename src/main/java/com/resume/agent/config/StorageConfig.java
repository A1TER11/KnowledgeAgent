package com.resume.agent.config;

import com.resume.agent.shared.store.ChatMessageStore;
import com.resume.agent.shared.store.DocumentStore;
import com.resume.agent.shared.store.InMemoryChatMessageStore;
import com.resume.agent.shared.store.InMemoryDocumentStore;
import com.resume.agent.shared.store.InMemoryMemoryStore;
import com.resume.agent.shared.store.MemoryStore;
import com.resume.agent.shared.store.jdbc.JdbcChatMessageStore;
import com.resume.agent.shared.store.jdbc.JdbcDocumentStore;
import com.resume.agent.shared.store.jdbc.JdbcMemoryStore;
import com.resume.agent.llm.EmbeddingClient;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;

@Configuration
public class StorageConfig {

    private static final Logger log = LoggerFactory.getLogger(StorageConfig.class);
    private static final int HNSW_MAX_DIMENSIONS = 2000;

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public DataSource postgresDataSource(AgentProperties properties) {
        DriverManagerDataSource dataSource = new DriverManagerDataSource();
        dataSource.setDriverClassName("org.postgresql.Driver");
        dataSource.setUrl(properties.getStorage().getPostgres().getUrl());
        dataSource.setUsername(properties.getStorage().getPostgres().getUsername());
        dataSource.setPassword(properties.getStorage().getPostgres().getPassword());
        return dataSource;
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public JdbcTemplate postgresJdbcTemplate(DataSource postgresDataSource) {
        return new JdbcTemplate(postgresDataSource);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public ChatMessageStore jdbcChatMessageStore(JdbcTemplate postgresJdbcTemplate) {
        return new JdbcChatMessageStore(postgresJdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public DocumentStore jdbcDocumentStore(JdbcTemplate postgresJdbcTemplate) {
        return new JdbcDocumentStore(postgresJdbcTemplate);
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public MemoryStore jdbcMemoryStore(JdbcTemplate postgresJdbcTemplate) {
        return new JdbcMemoryStore(postgresJdbcTemplate);
    }

    @Bean
    @ConditionalOnMissingBean(ChatMessageStore.class)
    public ChatMessageStore inMemoryChatMessageStore() {
        return new InMemoryChatMessageStore();
    }

    @Bean
    @ConditionalOnMissingBean(DocumentStore.class)
    public DocumentStore inMemoryDocumentStore() {
        return new InMemoryDocumentStore();
    }

    @Bean
    @ConditionalOnMissingBean(MemoryStore.class)
    public MemoryStore inMemoryMemoryStore() {
        return new InMemoryMemoryStore();
    }

    @Bean
    @ConditionalOnProperty(prefix = "agent.storage", name = "type", havingValue = "postgres")
    public CommandLineRunner postgresInitializer(JdbcTemplate postgresJdbcTemplate, EmbeddingClient embeddingClient) {
        return args -> {
            int embeddingDimensions = Math.max(1, embeddingClient.embed("dimension probe").size());
            log.info("Postgres storage initialization detected embedding dimensions: {}", embeddingDimensions);
            postgresJdbcTemplate.execute("CREATE EXTENSION IF NOT EXISTS vector");
            postgresJdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS chat_message (
                        id VARCHAR(64) PRIMARY KEY,
                        session_id VARCHAR(128) NOT NULL,
                        user_id VARCHAR(128) NOT NULL,
                        role VARCHAR(32) NOT NULL,
                        content TEXT NOT NULL,
                        created_at TIMESTAMP NOT NULL
                    )
                    """);
            postgresJdbcTemplate.execute("""
                    CREATE INDEX IF NOT EXISTS idx_chat_message_session_id
                    ON chat_message (session_id, created_at)
                    """);
            postgresJdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS knowledge_document (
                        document_id VARCHAR(64) PRIMARY KEY,
                        title VARCHAR(255) NOT NULL,
                        raw_content TEXT NOT NULL
                    )
                    """);
            postgresJdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS document_chunk ("
                            + "chunk_id VARCHAR(64) PRIMARY KEY,"
                            + "document_id VARCHAR(64) NOT NULL REFERENCES knowledge_document(document_id) ON DELETE CASCADE,"
                            + "title VARCHAR(255) NOT NULL,"
                            + "content TEXT NOT NULL,"
                            + "embedding vector(" + embeddingDimensions + ") NOT NULL"
                            + ")");
            postgresJdbcTemplate.execute(
                    "CREATE TABLE IF NOT EXISTS user_memory ("
                            + "memory_id VARCHAR(64) PRIMARY KEY,"
                            + "user_id VARCHAR(128) NOT NULL,"
                            + "memory_type VARCHAR(32) NOT NULL,"
                            + "content TEXT NOT NULL,"
                            + "source VARCHAR(64) NOT NULL,"
                            + "embedding vector(" + embeddingDimensions + ") NOT NULL,"
                            + "created_at TIMESTAMP NOT NULL"
                            + ")");
            if (embeddingDimensions <= HNSW_MAX_DIMENSIONS) {
                try {
                    postgresJdbcTemplate.execute("""
                            CREATE INDEX IF NOT EXISTS idx_document_chunk_embedding
                            ON document_chunk USING hnsw (embedding vector_cosine_ops)
                            """);
                    postgresJdbcTemplate.execute("""
                            CREATE INDEX IF NOT EXISTS idx_user_memory_embedding
                            ON user_memory USING hnsw (embedding vector_cosine_ops)
                            """);
                } catch (Exception exception) {
                    log.warn("Skip hnsw index creation during startup: {}", exception.getMessage());
                }
            } else {
                log.warn("Skip hnsw index creation during startup because embedding dimensions {} exceed pgvector hnsw limit {}.",
                        embeddingDimensions, HNSW_MAX_DIMENSIONS);
            }
        };
    }
}
