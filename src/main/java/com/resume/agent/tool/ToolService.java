package com.resume.agent.tool;

import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class ToolService {

    private static final Logger log = LoggerFactory.getLogger(ToolService.class);

    private final Map<String, ToolHandler> handlers;
    private final McpToolService mcpToolService;

    public ToolService(List<ToolHandler> handlers, McpToolService mcpToolService) {
        this.handlers = handlers.stream()
                .collect(Collectors.toMap(ToolHandler::name, Function.identity(), (left, right) -> left, LinkedHashMap::new));
        this.mcpToolService = mcpToolService;
    }

    public List<ToolExecutionRecord> runTools(ToolContext context, List<ToolCallRequest> requests) {
        List<ToolExecutionRecord> records = new ArrayList<>();
        for (ToolCallRequest request : requests) {
            ToolHandler handler = handlers.get(request.toolName());
            if (handler != null) {
                records.add(handler.execute(context, request));
                continue;
            }
            ToolExecutionRecord remoteRecord = mcpToolService.execute(context, request);
            if (remoteRecord != null) {
                records.add(remoteRecord);
            } else {
                log.warn("Tool {} was requested, but no local or MCP tool matched it.", request.toolName());
            }
        }
        return records;
    }

    public List<ToolDescriptor> toolCatalog() {
        List<ToolDescriptor> catalog = new ArrayList<>();
        for (ToolHandler handler : handlers.values()) {
            catalog.add(new ToolDescriptor(
                    handler.name(),
                    handler.description(),
                    handler.parameters(),
                    ToolSource.LOCAL
            ));
        }
        catalog.addAll(mcpToolService.toolCatalog());
        return catalog;
    }
}
