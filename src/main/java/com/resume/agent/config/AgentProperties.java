package com.resume.agent.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "agent")
public class AgentProperties {

    private final Memory memory = new Memory();
    private final Retrieval retrieval = new Retrieval();
    private final Llm llm = new Llm();
    private final Storage storage = new Storage();
    private final Mcp mcp = new Mcp();

    public Memory getMemory() {
        return memory;
    }

    public Retrieval getRetrieval() {
        return retrieval;
    }

    public Llm getLlm() {
        return llm;
    }

    public Storage getStorage() {
        return storage;
    }

    public Mcp getMcp() {
        return mcp;
    }

    public static class Memory {
        @Min(1)
        private int shortTermWindow = 8;

        public int getShortTermWindow() {
            return shortTermWindow;
        }

        public void setShortTermWindow(int shortTermWindow) {
            this.shortTermWindow = shortTermWindow;
        }
    }

    public static class Retrieval {
        @Min(1)
        private int topK = 3;
        private double minScore = 0.45d;

        public int getTopK() {
            return topK;
        }

        public void setTopK(int topK) {
            this.topK = topK;
        }

        public double getMinScore() {
            return minScore;
        }

        public void setMinScore(double minScore) {
            this.minScore = minScore;
        }
    }

    public static class Llm {
        private String baseUrl = "https://api.deepseek.com";
        private String apiKey = "replace-me";
        private String chatModel = "deepseek-v4-pro";
        private String embeddingModel = "text-embedding-3-small";
        private String embeddingBaseUrl = "https://api.openai.com/v1";
        private String embeddingApiKey = "replace-me";
        private boolean remoteChatEnabled;
        private boolean remoteEmbeddingEnabled;
        private boolean localEmbeddingFallbackEnabled;
        private String reasoningEffort = "high";
        private boolean thinkingEnabled = true;
        private int maxTokens = 1200;

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }

        public String getChatModel() {
            return chatModel;
        }

        public void setChatModel(String chatModel) {
            this.chatModel = chatModel;
        }

        public String getEmbeddingModel() {
            return embeddingModel;
        }

        public void setEmbeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
        }

        public String getEmbeddingBaseUrl() {
            return embeddingBaseUrl;
        }

        public void setEmbeddingBaseUrl(String embeddingBaseUrl) {
            this.embeddingBaseUrl = embeddingBaseUrl;
        }

        public String getEmbeddingApiKey() {
            return embeddingApiKey;
        }

        public void setEmbeddingApiKey(String embeddingApiKey) {
            this.embeddingApiKey = embeddingApiKey;
        }

        public boolean isRemoteChatEnabled() {
            return remoteChatEnabled;
        }

        public void setRemoteChatEnabled(boolean remoteChatEnabled) {
            this.remoteChatEnabled = remoteChatEnabled;
        }

        public boolean isRemoteChatConfigured() {
            return remoteChatEnabled
                    && apiKey != null
                    && !apiKey.isBlank()
                    && !"replace-me".equals(apiKey);
        }

        public boolean isRemoteEmbeddingEnabled() {
            return remoteEmbeddingEnabled;
        }

        public void setRemoteEmbeddingEnabled(boolean remoteEmbeddingEnabled) {
            this.remoteEmbeddingEnabled = remoteEmbeddingEnabled;
        }

        public boolean isRemoteEmbeddingConfigured() {
            return remoteEmbeddingEnabled
                    && embeddingApiKey != null
                    && !embeddingApiKey.isBlank()
                    && !"replace-me".equals(embeddingApiKey);
        }

        public boolean isLocalEmbeddingFallbackEnabled() {
            return localEmbeddingFallbackEnabled;
        }

        public void setLocalEmbeddingFallbackEnabled(boolean localEmbeddingFallbackEnabled) {
            this.localEmbeddingFallbackEnabled = localEmbeddingFallbackEnabled;
        }

        public String getReasoningEffort() {
            return reasoningEffort;
        }

        public void setReasoningEffort(String reasoningEffort) {
            this.reasoningEffort = reasoningEffort;
        }

        public boolean isThinkingEnabled() {
            return thinkingEnabled;
        }

        public void setThinkingEnabled(boolean thinkingEnabled) {
            this.thinkingEnabled = thinkingEnabled;
        }

        public int getMaxTokens() {
            return maxTokens;
        }

        public void setMaxTokens(int maxTokens) {
            this.maxTokens = maxTokens;
        }
    }

    public static class Storage {
        private String type = "memory";
        private final Postgres postgres = new Postgres();

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public Postgres getPostgres() {
            return postgres;
        }
    }

    public static class Mcp {
        private boolean enabled;
        private String serverName = "github";
        private String transport = "streamable-http";
        private String endpoint = "http://localhost:8081/mcp";
        private String authToken = "";
        private int timeoutMillis = 15000;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getServerName() {
            return serverName;
        }

        public void setServerName(String serverName) {
            this.serverName = serverName;
        }

        public String getTransport() {
            return transport;
        }

        public void setTransport(String transport) {
            this.transport = transport;
        }

        public String getEndpoint() {
            return endpoint;
        }

        public void setEndpoint(String endpoint) {
            this.endpoint = endpoint;
        }

        public String getAuthToken() {
            return authToken;
        }

        public void setAuthToken(String authToken) {
            this.authToken = authToken;
        }

        public int getTimeoutMillis() {
            return timeoutMillis;
        }

        public void setTimeoutMillis(int timeoutMillis) {
            this.timeoutMillis = timeoutMillis;
        }

        public boolean isConfigured() {
            return enabled && endpoint != null && !endpoint.isBlank();
        }
    }

    public static class Postgres {
        private String url = "jdbc:postgresql://localhost:5432/knowledge_agent";
        private String username = "postgres";
        private String password = "postgres";

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
