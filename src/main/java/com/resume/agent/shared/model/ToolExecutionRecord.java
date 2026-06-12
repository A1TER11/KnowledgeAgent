package com.resume.agent.shared.model;

public record ToolExecutionRecord(
        String toolName,
        String summary,
        String details
) {
}
