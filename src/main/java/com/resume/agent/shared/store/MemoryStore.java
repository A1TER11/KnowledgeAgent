package com.resume.agent.shared.store;

import com.resume.agent.shared.model.LongTermMemory;
import java.util.List;

public interface MemoryStore {
    LongTermMemory save(LongTermMemory memory);

    List<LongTermMemory> findByUserId(String userId);

    List<LongTermMemory> searchByEmbedding(String userId, List<Double> embedding, int topK, double minScore);
}
