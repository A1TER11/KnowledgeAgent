package com.resume.agent.tool;

import java.util.Map;

public record ToolCallRequest(
        String callId,
        String toolName,
        Map<String, Object> arguments
) {
    public ToolCallRequest {
        arguments = arguments == null ? Map.of() : Map.copyOf(arguments);
    }
}
