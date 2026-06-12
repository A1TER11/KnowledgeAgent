package com.resume.agent.tool;

public record ToolContext(
        String sessionId,
        String userId,
        String message
) {
}
