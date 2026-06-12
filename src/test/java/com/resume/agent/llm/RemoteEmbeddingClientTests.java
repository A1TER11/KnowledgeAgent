package com.resume.agent.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.resume.agent.config.AgentProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

class RemoteEmbeddingClientTests {

    @Test
    void shouldFailFastWhenRemoteEmbeddingDisabledAndLocalFallbackDisabled() {
        AgentProperties properties = new AgentProperties();
        properties.getLlm().setRemoteEmbeddingEnabled(false);
        properties.getLlm().setLocalEmbeddingFallbackEnabled(false);

        RemoteEmbeddingClient client = new RemoteEmbeddingClient(
                RestClient.builder(),
                properties,
                new SimpleEmbeddingClient());

        ResponseStatusException exception =
                assertThrows(ResponseStatusException.class, () -> client.embed("test"));

        assertEquals(503, exception.getStatusCode().value());
        assertEquals("Remote embedding is disabled or not configured.", exception.getReason());
    }
}
