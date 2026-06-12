package com.resume.agent.chat.api;

import java.util.List;
import java.util.Map;

public record ToolCatalogResponse(
        McpStatusView mcp,
        List<ToolView> tools
) {
    public record McpStatusView(
            boolean enabled,
            boolean configured,
            String serverName,
            String transport,
            String endpoint,
            boolean authTokenPresent,
            String lastError,
            int mcpToolCount
    ) {
    }

    public record ToolView(
            String name,
            String description,
            String source,
            Map<String, Object> parameters
    ) {
    }
}
