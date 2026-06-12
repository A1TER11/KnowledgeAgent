package com.resume.agent.shared.store;

import com.resume.agent.shared.model.KnowledgeDocument;
import com.resume.agent.rag.Similarity;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;

public class InMemoryDocumentStore implements DocumentStore {

    private final CopyOnWriteArrayList<KnowledgeDocument> documents = new CopyOnWriteArrayList<>();

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        documents.removeIf(existing -> existing.documentId().equals(document.documentId()));
        documents.add(document);
        return document;
    }

    @Override
    public List<KnowledgeDocument> findAll() {
        return List.copyOf(documents);
    }

    @Override
    public Optional<KnowledgeDocument> findById(String documentId) {
        return documents.stream().filter(document -> document.documentId().equals(documentId)).findFirst();
    }

    @Override
    public List<KnowledgeDocument> searchByEmbedding(List<Double> embedding, int topK, double minScore) {
        return documents.stream()
                .filter(document -> document.chunks().stream()
                        .anyMatch(chunk -> Similarity.cosine(embedding, chunk.embedding()) >= minScore))
                .sorted(Comparator.comparingDouble(
                        document -> -document.chunks().stream()
                                .mapToDouble(chunk -> Similarity.cosine(embedding, chunk.embedding()))
                                .max()
                                .orElse(0d)))
                .limit(topK)
                .toList();
    }

    @Override
    public void deleteById(String documentId) {
        documents.removeIf(document -> document.documentId().equals(documentId));
    }

    @Override
    public boolean existsByTitle(String title) {
        return documents.stream().anyMatch(document -> document.title().equals(title));
    }
}
