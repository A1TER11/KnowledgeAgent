package com.resume.agent.tool;

import com.resume.agent.shared.model.ToolExecutionRecord;
import org.springframework.stereotype.Component;

@Component
public class CreateTodoTool implements ToolHandler {

    @Override
    public String name() {
        return "create_todo";
    }

    @Override
    public String description() {
        return "Create a concise todo summary for the current request or project.";
    }

    @Override
    public ToolExecutionRecord execute(ToolContext context, ToolCallRequest request) {
        return new ToolExecutionRecord(
                name(),
                "Created a short action list for the current request.",
                "- Review the current knowledge base coverage\n"
                        + "- Add or refresh missing supporting documents\n"
                        + "- Validate the main demo questions end-to-end"
        );
    }
}
