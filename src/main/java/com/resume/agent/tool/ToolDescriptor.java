package com.resume.agent.tool;

import java.util.Map;

public record ToolDescriptor(
        String name,
        String description,
        Map<String, Object> parameters,
        ToolSource source
) {
    public ToolDescriptor {
        parameters = parameters == null ? Map.of("type", "object", "properties", Map.of()) : Map.copyOf(parameters);
    }
}
