package com.resume.agent.memory;

import com.resume.agent.llm.EmbeddingClient;
import com.resume.agent.shared.model.LongTermMemory;
import com.resume.agent.shared.model.MemoryType;
import com.resume.agent.shared.store.MemoryStore;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class LongTermMemoryService {

    private final MemoryStore memoryStore;
    private final EmbeddingClient embeddingClient;

    public LongTermMemoryService(MemoryStore memoryStore, EmbeddingClient embeddingClient) {
        this.memoryStore = memoryStore;
        this.embeddingClient = embeddingClient;
    }

    public LongTermMemory remember(String userId, MemoryType memoryType, String content, String source) {
        LongTermMemory memory = LongTermMemory.create(userId, memoryType, content, source, embeddingClient.embed(content));
        return memoryStore.save(memory);
    }

    public List<LongTermMemory> listMemories(String userId) {
        return memoryStore.findByUserId(userId);
    }

    public List<MemorySearchResult> searchRelevant(String userId, String message, int topK) {
        List<Double> target = embeddingClient.embed(message);
        return memoryStore.searchByEmbedding(userId, target, topK, 0.45d).stream()
                .map(memory -> new MemorySearchResult(
                        memory.memoryId(),
                        memory.memoryType(),
                        memory.content(),
                        com.resume.agent.rag.Similarity.cosine(target, memory.embedding())))
                .toList();
    }
}
