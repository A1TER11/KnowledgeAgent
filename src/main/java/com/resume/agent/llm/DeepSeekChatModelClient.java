package com.resume.agent.llm;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.agent.AgentDecision;
import com.resume.agent.config.AgentProperties;
import com.resume.agent.memory.MemorySearchResult;
import com.resume.agent.rag.RetrievedChunk;
import com.resume.agent.shared.model.ChatMessage;
import com.resume.agent.shared.model.MessageRole;
import com.resume.agent.shared.model.ToolExecutionRecord;
import com.resume.agent.tool.ToolCallRequest;
import com.resume.agent.tool.ToolContext;
import com.resume.agent.tool.ToolDescriptor;
import com.resume.agent.tool.ToolService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Primary
@Component
public class DeepSeekChatModelClient implements ChatModelClient {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekChatModelClient.class);

    private final RestClient restClient;
    private final AgentProperties properties;
    private final FallbackChatModelClient fallbackChatModelClient;
    private final ToolService toolService;
    private final ObjectMapper objectMapper;

    public DeepSeekChatModelClient(
            RestClient.Builder restClientBuilder,
            AgentProperties properties,
            FallbackChatModelClient fallbackChatModelClient,
            ToolService toolService,
            ObjectMapper objectMapper
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.fallbackChatModelClient = fallbackChatModelClient;
        this.toolService = toolService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ChatModelResult answer(AgentDecision decision) {
        if (!isRemoteEnabled()) {
            return fallbackChatModelClient.answer(decision);
        }

        try {
            List<DeepSeekMessage> messages = buildMessages(decision, decision.toolExecutions());
            DeepSeekChatResponse response = sendChatRequest(messages, buildTools());

            if (response == null || response.choices() == null || response.choices().isEmpty()) {
                log.warn("DeepSeek returned an empty response, fallback will be used.");
                return fallbackChatModelClient.answer(decision);
            }

            Message responseMessage = response.choices().get(0).message();
            if (responseMessage == null) {
                return fallbackChatModelClient.answer(decision);
            }

            List<ToolCall> toolCalls = responseMessage.toolCalls() == null ? List.of() : responseMessage.toolCalls();
            if (!toolCalls.isEmpty()) {
                return answerWithToolCalls(decision, messages, responseMessage, toolCalls);
            }

            String content = safeContent(responseMessage);
            return content == null || content.isBlank()
                    ? fallbackChatModelClient.answer(decision)
                    : new ChatModelResult(content, decision.toolExecutions());
        } catch (Exception exception) {
            log.warn("DeepSeek request failed, fallback will be used: {}", exception.getMessage());
            return fallbackChatModelClient.answer(decision);
        }
    }

    private ChatModelResult answerWithToolCalls(
            AgentDecision decision,
            List<DeepSeekMessage> messages,
            Message responseMessage,
            List<ToolCall> toolCalls
    ) {
        try {
            List<ToolCallRequest> requests = toolCalls.stream()
                    .map(this::toToolCallRequest)
                    .filter(request -> request != null && request.toolName() != null && !request.toolName().isBlank())
                    .toList();

            if (requests.isEmpty()) {
                String content = safeContent(responseMessage);
                return content == null || content.isBlank()
                        ? fallbackChatModelClient.answer(decision)
                        : new ChatModelResult(content, decision.toolExecutions());
            }

            List<ToolExecutionRecord> records = toolService.runTools(
                    new ToolContext(decision.sessionId(), decision.userId(), decision.userMessage()),
                    requests
            );

            if (records.isEmpty()) {
                log.warn("Model requested tools {}, but no tool records were produced.", requests.stream()
                        .map(ToolCallRequest::toolName)
                        .toList());
                return fallbackChatModelClient.answer(decision);
            }

            List<DeepSeekMessage> followUpMessages = new ArrayList<>(messages);
            followUpMessages.add(DeepSeekMessage.assistantToolCall(responseMessage.toolCalls()));
            followUpMessages.addAll(buildToolResultMessages(toolCalls, records));

            DeepSeekChatResponse finalResponse = sendChatRequest(followUpMessages, List.of());
            if (finalResponse == null || finalResponse.choices() == null || finalResponse.choices().isEmpty()) {
                return fallbackChatModelClient.answer(withToolRecords(decision, records));
            }

            String finalContent = safeContent(finalResponse.choices().get(0).message());
            if (finalContent == null || finalContent.isBlank()) {
                return fallbackChatModelClient.answer(withToolRecords(decision, records));
            }
            return new ChatModelResult(finalContent, records);
        } catch (Exception exception) {
            log.warn("Tool calling flow failed, retrying without tools: {}", exception.getMessage());
            List<DeepSeekMessage> fallbackMessages = buildMessages(decision, List.of());
            DeepSeekChatResponse retryResponse = sendChatRequest(fallbackMessages, List.of());
            if (retryResponse == null || retryResponse.choices() == null || retryResponse.choices().isEmpty()) {
                return fallbackChatModelClient.answer(decision);
            }
            String content = safeContent(retryResponse.choices().get(0).message());
            return content == null || content.isBlank()
                    ? fallbackChatModelClient.answer(decision)
                    : new ChatModelResult(content, List.of());
        }
    }

    private AgentDecision withToolRecords(AgentDecision decision, List<ToolExecutionRecord> records) {
        return new AgentDecision(
                decision.sessionId(),
                decision.userId(),
                decision.userMessage(),
                decision.recentMessages(),
                decision.knowledgeHits(),
                decision.memoryHits(),
                records
        );
    }

    private DeepSeekChatResponse sendChatRequest(List<DeepSeekMessage> messages, List<ToolDefinition> tools) {
        return restClient.post()
                .uri(properties.getLlm().getBaseUrl() + "/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getLlm().getApiKey())
                .contentType(MediaType.APPLICATION_JSON)
                .body(new DeepSeekChatRequest(
                        properties.getLlm().getChatModel(),
                        messages,
                        new ThinkingConfig(properties.getLlm().isThinkingEnabled() ? "enabled" : "disabled"),
                        properties.getLlm().getReasoningEffort(),
                        properties.getLlm().getMaxTokens(),
                        false,
                        tools == null || tools.isEmpty() ? null : tools
                ))
                .retrieve()
                .body(DeepSeekChatResponse.class);
    }

    private List<DeepSeekMessage> buildToolResultMessages(List<ToolCall> toolCalls, List<ToolExecutionRecord> records) {
        List<DeepSeekMessage> toolMessages = new ArrayList<>();
        int count = Math.min(toolCalls.size(), records.size());
        for (int index = 0; index < count; index++) {
            ToolCall toolCall = toolCalls.get(index);
            ToolExecutionRecord record = records.get(index);
            if (toolCall == null || toolCall.id() == null || record == null) {
                continue;
            }
            toolMessages.add(DeepSeekMessage.toolResult(toolCall.id(), record.summary() + "\n" + record.details()));
        }
        return toolMessages;
    }

    private List<DeepSeekMessage> buildMessages(AgentDecision decision, List<ToolExecutionRecord> records) {
        List<DeepSeekMessage> messages = new ArrayList<>();
        messages.add(DeepSeekMessage.system(buildSystemPrompt(decision, records)));
        for (ChatMessage recentMessage : decision.recentMessages()) {
            messages.add(new DeepSeekMessage(roleName(recentMessage.role()), recentMessage.content(), null, null));
        }
        messages.add(DeepSeekMessage.user(decision.userMessage()));
        return messages;
    }

    private List<ToolDefinition> buildTools() {
        List<ToolDefinition> tools = new ArrayList<>();
        for (ToolDescriptor descriptor : toolService.toolCatalog()) {
            tools.add(new ToolDefinition(
                    "function",
                    new ToolDefinitionFunction(
                            descriptor.name(),
                            descriptor.description(),
                            descriptor.parameters()
                    )
            ));
        }
        return tools;
    }

    private ToolCallRequest toToolCallRequest(ToolCall toolCall) {
        if (toolCall == null || toolCall.function() == null || toolCall.function().name() == null) {
            return null;
        }
        Map<String, Object> arguments = Map.of();
        try {
            String rawArguments = toolCall.function().arguments();
            if (rawArguments != null && !rawArguments.isBlank()) {
                arguments = objectMapper.readValue(rawArguments, new TypeReference<Map<String, Object>>() {
                });
            }
        } catch (Exception exception) {
            log.warn("Failed to parse tool arguments for {}: {}", toolCall.function().name(), exception.getMessage());
        }
        return new ToolCallRequest(toolCall.id(), toolCall.function().name(), arguments);
    }

    private boolean isRemoteEnabled() {
        return properties.getLlm().isRemoteChatEnabled()
                && properties.getLlm().getApiKey() != null
                && !properties.getLlm().getApiKey().isBlank()
                && !"replace-me".equals(properties.getLlm().getApiKey());
    }

    private String buildSystemPrompt(AgentDecision decision, List<ToolExecutionRecord> toolRecords) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("你是企业知识库助手。请严格基于提供的知识库证据和记忆内容作答，不要编造。");
        prompt.append("\n如果证据不足，请明确回答“信息不足”，不要假装知道答案。");
        prompt.append("\n如果命中了制度、时间、报销、考勤类内容，优先直接引用命中的事实再总结。");

        if (!decision.knowledgeHits().isEmpty()) {
            prompt.append("\n\n知识库命中：");
            appendKnowledgeHits(prompt, decision.knowledgeHits());
        }

        if (!decision.memoryHits().isEmpty()) {
            prompt.append("\n\n长期记忆命中：");
            appendMemoryHits(prompt, decision.memoryHits());
        }

        if (!toolRecords.isEmpty()) {
            prompt.append("\n\n工具执行结果：");
            appendToolExecutions(prompt, toolRecords);
        }
        return prompt.toString();
    }

    private void appendKnowledgeHits(StringBuilder prompt, List<RetrievedChunk> hits) {
        for (RetrievedChunk hit : hits) {
            prompt.append("\n- [").append(hit.title()).append("] ")
                    .append(hit.content())
                    .append(" (score=").append(String.format("%.3f", hit.score())).append(")");
        }
    }

    private void appendMemoryHits(StringBuilder prompt, List<MemorySearchResult> hits) {
        for (MemorySearchResult hit : hits) {
            prompt.append("\n- [").append(hit.memoryType()).append("] ").append(hit.content());
        }
    }

    private void appendToolExecutions(StringBuilder prompt, List<ToolExecutionRecord> records) {
        for (ToolExecutionRecord record : records) {
            prompt.append("\n- [").append(record.toolName()).append("] ")
                    .append(record.summary())
                    .append("\n")
                    .append(record.details());
        }
    }

    private String roleName(MessageRole role) {
        return role == MessageRole.USER ? "user" : "assistant";
    }

    private String safeContent(Message message) {
        return message == null ? null : message.content();
    }

    record DeepSeekChatRequest(
            String model,
            List<DeepSeekMessage> messages,
            ThinkingConfig thinking,
            @JsonProperty("reasoning_effort") String reasoningEffort,
            @JsonProperty("max_tokens") int maxTokens,
            boolean stream,
            List<ToolDefinition> tools
    ) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record DeepSeekMessage(
            String role,
            String content,
            @JsonProperty("tool_call_id") String toolCallId,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls
    ) {
        static DeepSeekMessage user(String content) {
            return new DeepSeekMessage("user", content, null, null);
        }

        static DeepSeekMessage system(String content) {
            return new DeepSeekMessage("system", content, null, null);
        }

        static DeepSeekMessage assistantToolCall(List<ToolCall> toolCalls) {
            return new DeepSeekMessage("assistant", null, null, toolCalls);
        }

        static DeepSeekMessage toolResult(String toolCallId, String content) {
            return new DeepSeekMessage("tool", content, toolCallId, null);
        }
    }

    record ThinkingConfig(String type) {
    }

    record ToolDefinition(String type, ToolDefinitionFunction function) {
    }

    record ToolDefinitionFunction(String name, String description, Map<String, Object> parameters) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record DeepSeekChatResponse(List<Choice> choices) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(Message message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Message(
            @JsonAlias("reasoning_content") String reasoningContent,
            String content,
            @JsonProperty("tool_calls") List<ToolCall> toolCalls
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolCall(String id, String type, ToolFunction function) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolFunction(String name, String arguments) {
    }
}
