package com.resume.agent.rag;

import java.util.List;

public record SearchResult(
        List<RetrievedChunk> hits,
        SearchDiagnostics diagnostics
) {
    public record SearchDiagnostics(
            String query,
            int titleHits,
            int keywordHits,
            int vectorHits
    ) {
    }
}
