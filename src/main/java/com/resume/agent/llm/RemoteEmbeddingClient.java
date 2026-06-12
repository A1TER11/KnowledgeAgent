package com.resume.agent.llm;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.resume.agent.config.AgentProperties;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Primary
@Component
public class RemoteEmbeddingClient implements EmbeddingClient {

    private static final Logger log = LoggerFactory.getLogger(RemoteEmbeddingClient.class);

    private final RestClient restClient;
    private final AgentProperties properties;
    private final SimpleEmbeddingClient fallbackEmbeddingClient;

    public RemoteEmbeddingClient(
            RestClient.Builder restClientBuilder,
            AgentProperties properties,
            SimpleEmbeddingClient fallbackEmbeddingClient
    ) {
        this.restClient = restClientBuilder.build();
        this.properties = properties;
        this.fallbackEmbeddingClient = fallbackEmbeddingClient;
    }

    @Override
    public List<Double> embed(String text) {
        if (!isRemoteEnabled()) {
            return fallbackOrThrow("Remote embedding is disabled or not configured.", null, text);
        }

        try {
            EmbeddingResponse response = restClient.post()
                    .uri(properties.getLlm().getEmbeddingBaseUrl() + "/embeddings")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getLlm().getEmbeddingApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(new EmbeddingRequest(properties.getLlm().getEmbeddingModel(), text))
                    .retrieve()
                    .body(EmbeddingResponse.class);

            if (response == null || response.data() == null || response.data().isEmpty()) {
                return fallbackOrThrow("Embedding API returned an empty response.", null, text);
            }

            List<Double> embedding = response.data().get(0).embedding();
            if ("dimension probe".equals(text) && embedding != null) {
                log.info(
                        "Remote embedding probe succeeded with model '{}' at '{}' and returned {} dimensions.",
                        properties.getLlm().getEmbeddingModel(),
                        properties.getLlm().getEmbeddingBaseUrl(),
                        embedding.size()
                );
            }
            return embedding == null || embedding.isEmpty()
                    ? fallbackOrThrow("Embedding API returned an empty embedding vector.", null, text)
                    : embedding;
        } catch (RestClientException exception) {
            return fallbackOrThrow("Embedding request failed.", exception, text);
        }
    }

    private List<Double> fallbackOrThrow(String message, Exception exception, String text) {
        if (properties.getLlm().isLocalEmbeddingFallbackEnabled()) {
            if (exception == null) {
                log.warn("{} Falling back to local embedding.", message);
            } else {
                log.warn("{} Falling back to local embedding: {}", message, exception.getMessage());
            }
            return fallbackEmbeddingClient.embed(text);
        }

        if (exception == null) {
            log.error("{} Local embedding fallback is disabled.", message);
        } else {
            log.error("{} Local embedding fallback is disabled: {}", message, exception.getMessage());
        }
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, message);
    }

    private boolean isRemoteEnabled() {
        return properties.getLlm().isRemoteEmbeddingEnabled()
                && properties.getLlm().getEmbeddingApiKey() != null
                && !properties.getLlm().getEmbeddingApiKey().isBlank()
                && !"replace-me".equals(properties.getLlm().getEmbeddingApiKey());
    }

    record EmbeddingRequest(String model, String input) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingResponse(List<EmbeddingData> data) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record EmbeddingData(List<Double> embedding) {
    }
}
