package com.resume.agent.tool;

import com.resume.agent.rag.RagService;
import com.resume.agent.rag.RetrievedChunk;
import com.resume.agent.rag.SearchResult;
import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

@Component
public class SearchDocsTool implements ToolHandler {

    private final RagService ragService;

    public SearchDocsTool(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public String name() {
        return "search_docs";
    }

    @Override
    public String description() {
        return "Search the knowledge base and return the most relevant evidence snippets.";
    }

    @Override
    public ToolExecutionRecord execute(ToolContext context, ToolCallRequest request) {
        Object queryArgument = request.arguments().get("query");
        String query = queryArgument == null || queryArgument.toString().isBlank()
                ? context.message()
                : queryArgument.toString();
        SearchResult result = ragService.searchDetailed(query);
        String summary = "Knowledge search returned " + result.hits().size()
                + " snippets (title=" + result.diagnostics().titleHits()
                + ", keyword=" + result.diagnostics().keywordHits()
                + ", vector=" + result.diagnostics().vectorHits() + ").";

        String details = result.hits().isEmpty()
                ? "No matching snippets were found for query: " + query
                : result.hits().stream()
                        .map(this::formatHit)
                        .collect(Collectors.joining("\n"));

        return new ToolExecutionRecord(name(), summary, details);
    }

    @Override
    public java.util.Map<String, Object> parameters() {
        return java.util.Map.of(
                "type", "object",
                "properties", java.util.Map.of(
                        "query", java.util.Map.of(
                                "type", "string",
                                "description", "Search query to use against the knowledge base."
                        )
                )
        );
    }

    private String formatHit(RetrievedChunk hit) {
        return "- [" + hit.title() + "] score=" + String.format("%.3f", hit.score()) + " " + hit.content();
    }
}
