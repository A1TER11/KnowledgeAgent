package com.resume.agent.rag;

import com.resume.agent.config.AgentProperties;
import com.resume.agent.docs.api.DocumentDetailView;
import com.resume.agent.docs.api.DocumentUploadRequest;
import com.resume.agent.docs.api.DocumentView;
import com.resume.agent.llm.EmbeddingClient;
import com.resume.agent.shared.model.DocumentChunk;
import com.resume.agent.shared.model.KnowledgeDocument;
import com.resume.agent.shared.store.DocumentStore;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RagService {

    private static final List<List<String>> KEYWORD_GROUPS = List.of(
            List.of("tech stack", "stack", "技术栈"),
            List.of("java"),
            List.of("spring", "spring boot"),
            List.of("postgres", "postgresql"),
            List.of("pgvector"),
            List.of("rag", "检索"),
            List.of("agent", "knowledge agent", "knowledge agent demo"),
            List.of("memory", "记忆"),
            List.of("embedding", "向量"),
            List.of("function calling", "tool", "工具"),
            List.of("deepseek"),
            List.of("员工手册", "手册"),
            List.of("报销", "费用", "发票", "补贴"),
            List.of("请假", "休假", "病假", "年假", "审批"),
            List.of("考勤", "工时", "出勤"),
            List.of("办公时间", "工作时间", "上班时间", "下班时间", "上下班", "上班", "下班", "几点上班", "几点下班", "什么时候上班", "什么时候下班")
    );

    private static final Set<String> STOP_TERMS = Set.of(
            "什么", "时候", "请问", "一个", "这个", "那个", "可以", "一下子", "我们", "你们", "关于", "是否"
    );

    private final DocumentStore documentStore;
    private final EmbeddingClient embeddingClient;
    private final AgentProperties properties;

    public RagService(DocumentStore documentStore, EmbeddingClient embeddingClient, AgentProperties properties) {
        this.documentStore = documentStore;
        this.embeddingClient = embeddingClient;
        this.properties = properties;
    }

    public DocumentView upload(DocumentUploadRequest request) {
        KnowledgeDocument document = buildDocument(java.util.UUID.randomUUID().toString(), request.title(), request.content());
        documentStore.save(document);
        return new DocumentView(document.documentId(), document.title(), document.chunks().size());
    }

    public List<DocumentView> listDocuments() {
        return documentStore.findAll().stream()
                .map(document -> new DocumentView(document.documentId(), document.title(), document.chunks().size()))
                .toList();
    }

    public DocumentDetailView documentDetail(String documentId) {
        KnowledgeDocument document = documentStore.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        return toDetailView(document);
    }

    public DocumentDetailView update(String documentId, DocumentUploadRequest request) {
        documentStore.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found"));
        KnowledgeDocument updated = buildDocument(documentId, request.title(), request.content());
        documentStore.save(updated);
        return toDetailView(updated);
    }

    public void delete(String documentId) {
        if (documentStore.findById(documentId).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Document not found");
        }
        documentStore.deleteById(documentId);
    }

    public List<RetrievedChunk> search(String question) {
        return searchDetailed(question).hits();
    }

    public SearchResult searchDetailed(String question) {
        Map<String, RetrievedChunk> mergedHits = new LinkedHashMap<>();

        List<RetrievedChunk> titleHits = titleSearch(question);
        mergeHits(mergedHits, titleHits);

        List<RetrievedChunk> keywordHits = keywordSearch(question);
        mergeHits(mergedHits, keywordHits);

        List<RetrievedChunk> vectorHits = vectorSearch(question);
        mergeHits(mergedHits, vectorHits);

        List<RetrievedChunk> sorted = mergedHits.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(properties.getRetrieval().getTopK())
                .toList();

        return new SearchResult(
                sorted,
                new SearchResult.SearchDiagnostics(
                        question,
                        titleHits.size(),
                        keywordHits.size(),
                        vectorHits.size()
                )
        );
    }

    public boolean hasDocumentWithTitle(String title) {
        return documentStore.existsByTitle(title);
    }

    public DocumentView uploadSeed(String title, String content) {
        KnowledgeDocument document = buildDocument(java.util.UUID.randomUUID().toString(), title, content);
        documentStore.save(document);
        return new DocumentView(document.documentId(), document.title(), document.chunks().size());
    }

    private void mergeHits(Map<String, RetrievedChunk> mergedHits, List<RetrievedChunk> hits) {
        for (RetrievedChunk hit : hits) {
            String key = hit.documentId() + "::" + hit.title() + "::" + hit.content();
            RetrievedChunk existing = mergedHits.get(key);
            if (existing == null || hit.score() > existing.score()) {
                mergedHits.put(key, hit);
            }
        }
    }

    private String buildEmbeddingText(String title, String content) {
        return title + "\n" + content;
    }

    private List<String> chunk(String content) {
        String[] paragraphs = content.split("\\r?\\n\\r?\\n");
        List<String> chunks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String paragraph : paragraphs) {
            String candidate = paragraph.trim();
            if (candidate.isEmpty()) {
                continue;
            }
            if (current.length() + candidate.length() > 240 && current.length() > 0) {
                chunks.add(current.toString());
                current = new StringBuilder();
            }
            if (current.length() > 0) {
                current.append("\n");
            }
            current.append(candidate);
        }
        if (current.length() > 0) {
            chunks.add(current.toString());
        }
        if (chunks.isEmpty()) {
            chunks.add(content);
        }
        return chunks;
    }

    private KnowledgeDocument buildDocument(String documentId, String title, String rawContent) {
        List<DocumentChunk> chunks = chunk(rawContent).stream()
                .map(content -> DocumentChunk.create(
                        documentId,
                        title,
                        content,
                        embeddingClient.embed(buildEmbeddingText(title, content))))
                .toList();
        return new KnowledgeDocument(documentId, title, rawContent, chunks);
    }

    private DocumentDetailView toDetailView(KnowledgeDocument document) {
        return new DocumentDetailView(
                document.documentId(),
                document.title(),
                document.rawContent(),
                document.chunks().size(),
                document.chunks().stream()
                        .map(chunk -> new DocumentDetailView.DocumentChunkView(
                                chunk.chunkId(),
                                chunk.title(),
                                chunk.content()))
                        .toList()
        );
    }

    private List<RetrievedChunk> titleSearch(String question) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion.isBlank()) {
            return List.of();
        }

        Map<String, RetrievedChunk> bestByChunkId = new LinkedHashMap<>();
        for (KnowledgeDocument document : documentStore.findAll()) {
            String normalizedTitle = normalize(document.title());
            if (normalizedTitle.isBlank()) {
                continue;
            }

            boolean exactMatch = normalizedQuestion.equals(normalizedTitle);
            boolean partialMatch = !exactMatch
                    && normalizedQuestion.length() >= 2
                    && (normalizedTitle.contains(normalizedQuestion) || normalizedQuestion.contains(normalizedTitle));
            if (!exactMatch && !partialMatch) {
                continue;
            }

            double score = exactMatch ? 1.25d : 1.05d;
            for (DocumentChunk chunk : document.chunks()) {
                bestByChunkId.put(chunk.chunkId(), new RetrievedChunk(
                        chunk.documentId(),
                        chunk.title(),
                        chunk.content(),
                        score));
            }
        }

        return bestByChunkId.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(properties.getRetrieval().getTopK())
                .toList();
    }

    private List<RetrievedChunk> keywordSearch(String question) {
        List<String> keywords = extractKeywords(question);
        if (keywords.isEmpty()) {
            return List.of();
        }

        Map<String, RetrievedChunk> bestByChunkId = new LinkedHashMap<>();
        for (KnowledgeDocument document : documentStore.findAll()) {
            for (DocumentChunk chunk : document.chunks()) {
                String haystack = normalize(document.title() + "\n" + chunk.title() + "\n" + chunk.content());
                int score = keywordScore(haystack, keywords);
                if (score <= 0) {
                    continue;
                }
                double normalizedScore = 0.9d + Math.min(0.45d, score * 0.1d) + heuristicBoost(normalize(question), haystack);
                RetrievedChunk hit = new RetrievedChunk(
                        chunk.documentId(),
                        chunk.title(),
                        chunk.content(),
                        normalizedScore);
                bestByChunkId.put(chunk.chunkId(), hit);
            }
        }

        return bestByChunkId.values().stream()
                .sorted((left, right) -> Double.compare(right.score(), left.score()))
                .limit(properties.getRetrieval().getTopK())
                .toList();
    }

    private List<RetrievedChunk> vectorSearch(String question) {
        try {
            List<Double> questionEmbedding = embeddingClient.embed(question);
            return documentStore.searchByEmbedding(
                            questionEmbedding,
                            Math.max(properties.getRetrieval().getTopK() * 2, properties.getRetrieval().getTopK()),
                            properties.getRetrieval().getMinScore())
                    .stream()
                    .flatMap(document -> document.chunks().stream())
                    .map(chunk -> new RetrievedChunk(
                            chunk.documentId(),
                            chunk.title(),
                            chunk.content(),
                            Similarity.cosine(questionEmbedding, chunk.embedding())))
                    .sorted((left, right) -> Double.compare(right.score(), left.score()))
                    .filter(chunk -> chunk.score() >= properties.getRetrieval().getMinScore())
                    .limit(properties.getRetrieval().getTopK())
                    .toList();
        } catch (Exception exception) {
            return List.of();
        }
    }

    private List<String> extractKeywords(String question) {
        String normalized = normalize(question);
        if (normalized.isBlank()) {
            return List.of();
        }

        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        for (List<String> group : KEYWORD_GROUPS) {
            boolean matched = false;
            for (String term : group) {
                if (normalized.contains(normalize(term))) {
                    matched = true;
                    break;
                }
            }
            if (matched) {
                for (String term : group) {
                    keywords.add(normalize(term));
                }
            }
        }

        for (String token : extractLiteralTokens(normalized)) {
            if (!STOP_TERMS.contains(token)) {
                keywords.add(token);
            }
        }
        return List.copyOf(keywords);
    }

    private List<String> extractLiteralTokens(String normalizedQuestion) {
        LinkedHashSet<String> tokens = new LinkedHashSet<>();
        for (String part : normalizedQuestion.split("[\\p{Punct}\\s]+")) {
            String token = part.trim();
            if (token.length() >= 2) {
                tokens.add(token);
            }
        }

        if (!normalizedQuestion.contains(" ")) {
            for (int i = 0; i < normalizedQuestion.length() - 1; i++) {
                String gram = normalizedQuestion.substring(i, i + 2).trim();
                if (gram.length() == 2 && !STOP_TERMS.contains(gram)) {
                    tokens.add(gram);
                }
            }
        }
        return List.copyOf(tokens);
    }

    private int keywordScore(String haystack, List<String> keywords) {
        int score = 0;
        for (String keyword : keywords) {
            if (haystack.contains(keyword)) {
                score++;
            }
        }
        return score;
    }

    private double heuristicBoost(String normalizedQuestion, String haystack) {
        double boost = 0d;
        if (containsAny(normalizedQuestion, "下班", "上班", "办公时间", "工作时间", "什么时候下班", "什么时候上班")) {
            if (containsAny(haystack, "办公时间", "工作时间", "09:30", "18:30")) {
                boost += 0.25d;
            }
        }
        if (containsAny(normalizedQuestion, "报销", "发票", "费用")) {
            if (containsAny(haystack, "报销", "发票", "费用", "补贴")) {
                boost += 0.2d;
            }
        }
        if (containsAny(normalizedQuestion, "请假", "病假", "年假", "审批")) {
            if (containsAny(haystack, "请假", "病假", "年假", "审批")) {
                boost += 0.2d;
            }
        }
        return boost;
    }

    private boolean containsAny(String text, String... candidates) {
        for (String candidate : candidates) {
            if (text.contains(candidate)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String text) {
        return text == null ? "" : text.trim().toLowerCase(Locale.ROOT);
    }
}
