package com.resume.agent.llm;

import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.List;

public record ChatModelResult(
        String answer,
        List<ToolExecutionRecord> toolExecutions
) {
}
