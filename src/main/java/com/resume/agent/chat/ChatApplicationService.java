package com.resume.agent.chat;

import com.resume.agent.agent.AgentDecision;
import com.resume.agent.agent.AgentOrchestrator;
import com.resume.agent.chat.api.ChatRequest;
import com.resume.agent.chat.api.ChatResponse;
import com.resume.agent.chat.api.SessionResponse;
import com.resume.agent.chat.api.ToolCatalogResponse;
import com.resume.agent.config.AgentProperties;
import com.resume.agent.llm.ChatModelClient;
import com.resume.agent.llm.ChatModelResult;
import com.resume.agent.shared.store.ChatMessageStore;
import com.resume.agent.tool.McpToolService;
import com.resume.agent.tool.ToolService;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatApplicationService {

    private final AgentOrchestrator agentOrchestrator;
    private final ChatModelClient chatModelClient;
    private final ChatMessageStore chatMessageStore;
    private final ToolService toolService;
    private final AgentProperties agentProperties;
    private final McpToolService mcpToolService;

    public ChatApplicationService(
            AgentOrchestrator agentOrchestrator,
            ChatModelClient chatModelClient,
            ChatMessageStore chatMessageStore,
            ToolService toolService,
            AgentProperties agentProperties,
            McpToolService mcpToolService
    ) {
        this.agentOrchestrator = agentOrchestrator;
        this.chatModelClient = chatModelClient;
        this.chatMessageStore = chatMessageStore;
        this.toolService = toolService;
        this.agentProperties = agentProperties;
        this.mcpToolService = mcpToolService;
    }

    public ChatResponse chat(ChatRequest request) {
        AgentDecision decision = agentOrchestrator.prepare(request.sessionId(), request.userId(), request.message());
        ChatModelResult result = chatModelClient.answer(decision);
        agentOrchestrator.recordAssistantAnswer(request.sessionId(), request.userId(), result.answer());

        return new ChatResponse(
                request.sessionId(),
                request.userId(),
                result.answer(),
                decision.knowledgeHits().stream()
                        .map(hit -> new ChatResponse.ContextSnippetView(hit.documentId(), hit.title(), hit.content(), hit.score()))
                        .toList(),
                decision.memoryHits().stream()
                        .map(hit -> new ChatResponse.MemoryView(hit.memoryId(), hit.memoryType().name(), hit.content(), hit.score()))
                        .toList(),
                result.toolExecutions().stream()
                        .map(record -> new ChatResponse.ToolCallView(record.toolName(), record.summary(), record.details()))
                        .toList()
        );
    }

    public SessionResponse session(String sessionId) {
        return new SessionResponse(
                sessionId,
                chatMessageStore.findBySessionId(sessionId).stream()
                        .map(message -> new SessionResponse.MessageView(
                                message.role().name(),
                                message.content(),
                                message.createdAt()))
                        .toList()
        );
    }

    public ToolCatalogResponse toolCatalog() {
        AgentProperties.Mcp mcp = agentProperties.getMcp();
        var catalog = toolService.toolCatalog();
        int mcpToolCount = (int) catalog.stream()
                .filter(tool -> "MCP".equals(tool.source().name()))
                .count();
        return new ToolCatalogResponse(
                new ToolCatalogResponse.McpStatusView(
                        mcp.isEnabled(),
                        mcp.isConfigured(),
                        mcp.getServerName(),
                        mcp.getTransport(),
                        mcp.getEndpoint(),
                        mcp.getAuthToken() != null && !mcp.getAuthToken().isBlank(),
                        mcpToolService.lastCatalogError(),
                        mcpToolCount
                ),
                catalog.stream()
                        .map(tool -> new ToolCatalogResponse.ToolView(
                                tool.name(),
                                tool.description(),
                                tool.source().name(),
                                tool.parameters()
                        ))
                        .toList()
        );
    }
}
