package com.resume.agent.shared.store;

import com.resume.agent.shared.model.LongTermMemory;
import com.resume.agent.rag.Similarity;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryMemoryStore implements MemoryStore {

    private final CopyOnWriteArrayList<LongTermMemory> memories = new CopyOnWriteArrayList<>();

    @Override
    public LongTermMemory save(LongTermMemory memory) {
        memories.add(memory);
        return memory;
    }

    @Override
    public List<LongTermMemory> findByUserId(String userId) {
        return memories.stream()
                .filter(memory -> memory.userId().equals(userId))
                .sorted(Comparator.comparing(LongTermMemory::createdAt).reversed())
                .toList();
    }

    @Override
    public List<LongTermMemory> searchByEmbedding(String userId, List<Double> embedding, int topK, double minScore) {
        return memories.stream()
                .filter(memory -> memory.userId().equals(userId))
                .filter(memory -> Similarity.cosine(embedding, memory.embedding()) >= minScore)
                .sorted(Comparator.comparingDouble(memory -> -Similarity.cosine(embedding, memory.embedding())))
                .limit(topK)
                .toList();
    }
}
