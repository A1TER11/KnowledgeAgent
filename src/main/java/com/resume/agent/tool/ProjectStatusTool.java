package com.resume.agent.tool;

import com.resume.agent.shared.model.ToolExecutionRecord;
import org.springframework.stereotype.Component;

@Component
public class ProjectStatusTool implements ToolHandler {

    @Override
    public String name() {
        return "get_project_status";
    }

    @Override
    public String description() {
        return "Summarize the current status of the project.";
    }

    @Override
    public ToolExecutionRecord execute(ToolContext context, ToolCallRequest request) {
        return new ToolExecutionRecord(
                name(),
                "Summarized the current project status.",
                "- RAG pipeline is enabled\n"
                        + "- Short-term and long-term memory are available\n"
                        + "- Tool calling is enabled for searchable project context"
        );
    }
}
