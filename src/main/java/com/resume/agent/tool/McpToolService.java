package com.resume.agent.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.resume.agent.config.AgentProperties;
import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
public class McpToolService {

    private static final Logger log = LoggerFactory.getLogger(McpToolService.class);

    private final RestClient restClient;
    private final AgentProperties properties;
    private final ObjectMapper objectMapper;
    private final AtomicLong requestId = new AtomicLong(1);

    private volatile boolean initialized;
    private volatile List<ToolDescriptor> cachedCatalog = List.of();
    private volatile String lastCatalogError;

    public McpToolService(RestClient.Builder restClientBuilder, AgentProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public List<ToolDescriptor> toolCatalog() {
        if (!properties.getMcp().isConfigured()) {
            lastCatalogError = "MCP is not configured.";
            return List.of();
        }
        if (properties.getMcp().getAuthToken() == null || properties.getMcp().getAuthToken().isBlank()) {
            lastCatalogError = "MCP auth token is missing.";
            return cachedCatalog;
        }
        try {
            ensureInitialized();
            RpcResponse<ToolsListResult> response = sendRpcRequest("tools/list", Map.of(), ToolsListResult.class);
            if (response == null) {
                lastCatalogError = "MCP server returned no response for tools/list.";
                return cachedCatalog;
            }
            if (response.error() != null) {
                lastCatalogError = "MCP server error " + response.error().code() + ": " + response.error().message();
                return cachedCatalog;
            }
            if (response.result() == null) {
                lastCatalogError = "MCP server returned no result for tools/list.";
                return cachedCatalog;
            }
            List<ToolDescriptor> descriptors = new ArrayList<>();
            if (response.result().tools() != null) {
                for (McpTool tool : response.result().tools()) {
                    descriptors.add(new ToolDescriptor(
                            tool.name(),
                            tool.description() == null ? "MCP tool exposed by " + properties.getMcp().getServerName() : tool.description(),
                            tool.inputSchema() == null ? Map.of("type", "object", "properties", Map.of()) : tool.inputSchema(),
                            ToolSource.MCP
                    ));
                }
            }
            cachedCatalog = List.copyOf(descriptors);
            lastCatalogError = null;
            return cachedCatalog;
        } catch (Exception exception) {
            lastCatalogError = exception.getMessage();
            log.warn("Failed to load MCP tool catalog from {}: {}", properties.getMcp().getServerName(), exception.getMessage());
            return cachedCatalog;
        }
    }

    public String lastCatalogError() {
        return lastCatalogError;
    }

    public ToolExecutionRecord execute(ToolContext context, ToolCallRequest request) {
        if (!properties.getMcp().isConfigured()) {
            return null;
        }
        List<ToolDescriptor> descriptors = cachedCatalog.isEmpty() ? toolCatalog() : cachedCatalog;
        boolean exists = descriptors.stream().anyMatch(tool -> tool.name().equals(request.toolName()));
        if (!exists) {
            return null;
        }

        try {
            ensureInitialized();
            RpcResponse<CallToolResult> response = sendRpcRequest(
                    "tools/call",
                    Map.of(
                            "name", request.toolName(),
                            "arguments", request.arguments()
                    ),
                    CallToolResult.class
            );

            if (response == null || response.result() == null) {
                return new ToolExecutionRecord(
                        request.toolName(),
                        "MCP tool returned an empty result.",
                        "Server: " + properties.getMcp().getServerName()
                );
            }
            if (response.error() != null) {
                return new ToolExecutionRecord(
                        request.toolName(),
                        "MCP tool execution failed.",
                        "Server: " + properties.getMcp().getServerName()
                                + "\nReason: " + response.error().message()
                );
            }

            String details = formatCallResult(response.result());
            String summary = response.result().isError()
                    ? "MCP tool execution reported an error."
                    : "MCP tool executed successfully via " + properties.getMcp().getServerName() + ".";

            return new ToolExecutionRecord(request.toolName(), summary, details);
        } catch (Exception exception) {
            log.warn("Failed to execute MCP tool {}: {}", request.toolName(), exception.getMessage());
            return new ToolExecutionRecord(
                    request.toolName(),
                    "MCP tool execution failed.",
                    "Server: " + properties.getMcp().getServerName() + "\nReason: " + exception.getMessage()
            );
        }
    }

    private void ensureInitialized() {
        if (initialized) {
            return;
        }
        synchronized (this) {
            if (initialized) {
                return;
            }
            RpcResponse<JsonNode> response = sendRpcRequest("initialize", Map.of(
                    "protocolVersion", "2024-11-05",
                    "capabilities", Map.of(),
                    "clientInfo", Map.of("name", "knowledge-agent", "version", "0.0.1")
            ), JsonNode.class);
            if (response == null) {
                throw new IllegalStateException("MCP server returned no response for initialize.");
            }
            if (response.error() != null) {
                throw new IllegalStateException("MCP initialize failed: " + response.error().message());
            }
            initialized = true;
        }
    }

    private <T> RpcResponse<T> sendRpcRequest(String method, Map<String, Object> params, Class<T> resultType) {
        String token = properties.getMcp().getAuthToken();
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON, MediaType.TEXT_EVENT_STREAM));
        if (token != null && !token.isBlank()) {
            headers.setBearerAuth(token);
        }

        String responseBody = restClient.post()
                .uri(properties.getMcp().getEndpoint())
                .headers(httpHeaders -> httpHeaders.addAll(headers))
                .body(new RpcRequest(
                        "2.0",
                        requestId.getAndIncrement(),
                        method,
                        params
                ))
                .retrieve()
                .body(String.class);
        if (responseBody == null) {
            return null;
        }
        JsonNode responseNode = extractRpcResponse(responseBody);
        return objectMapper.convertValue(
                responseNode,
                objectMapper.getTypeFactory().constructParametricType(RpcResponse.class, resultType)
        );
    }

    private JsonNode extractRpcResponse(String responseBody) {
        String trimmed = responseBody.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.readTree(trimmed);
        } catch (Exception ignored) {
            return parseSseResponse(trimmed);
        }
    }

    private JsonNode parseSseResponse(String responseBody) {
        String[] events = responseBody.split("\\R\\R");
        JsonNode lastJsonNode = null;
        for (String event : events) {
            StringBuilder data = new StringBuilder();
            String[] lines = event.split("\\R");
            for (String line : lines) {
                if (line.startsWith("data:")) {
                    if (!data.isEmpty()) {
                        data.append("\n");
                    }
                    data.append(line.substring(5).trim());
                }
            }
            if (data.isEmpty()) {
                continue;
            }
            String payload = data.toString().trim();
            if (payload.isEmpty() || "[DONE]".equals(payload)) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(payload);
                if (node.has("jsonrpc")) {
                    lastJsonNode = node;
                }
            } catch (Exception ignored) {
                log.debug("Skipping non-JSON SSE payload from MCP server: {}", payload);
            }
        }
        if (lastJsonNode == null) {
            throw new IllegalStateException("MCP server returned SSE without a JSON-RPC payload.");
        }
        return lastJsonNode;
    }

    private String formatCallResult(CallToolResult result) {
        if (result.content() == null || result.content().isEmpty()) {
            return "No structured content was returned by the MCP server.";
        }
        StringBuilder builder = new StringBuilder();
        for (CallToolContentItem item : result.content()) {
            if (item == null) {
                continue;
            }
            if (item.text() != null && !item.text().isBlank()) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(item.text());
            } else if (item.data() != null && !item.data().isMissingNode()) {
                if (!builder.isEmpty()) {
                    builder.append("\n");
                }
                builder.append(item.data().toPrettyString());
            }
        }
        if (builder.isEmpty()) {
            return "MCP tool completed but returned no text payload.";
        }
        return builder.toString();
    }

    record RpcRequest(String jsonrpc, long id, String method, Map<String, Object> params) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RpcResponse<T>(T result, RpcError error) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record RpcError(int code, String message) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ToolsListResult(List<McpTool> tools) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record McpTool(
            String name,
            String description,
            @JsonInclude(JsonInclude.Include.NON_NULL) Map<String, Object> inputSchema
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CallToolResult(List<CallToolContentItem> content, boolean isError) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CallToolContentItem(String type, String text, JsonNode data) {
    }
}
