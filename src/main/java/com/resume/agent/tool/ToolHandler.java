package com.resume.agent.tool;

import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.Map;

public interface ToolHandler {
    String name();

    String description();

    default Map<String, Object> parameters() {
        return Map.of("type", "object", "properties", Map.of());
    }

    ToolExecutionRecord execute(ToolContext context, ToolCallRequest request);
}
