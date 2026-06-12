package com.resume.agent.config;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/meta")
public class AppMetaController {

    private final AgentProperties properties;

    public AppMetaController(AgentProperties properties) {
        this.properties = properties;
    }

    @GetMapping
    public AppMetaResponse overview() {
        boolean remoteChatEnabled = properties.getLlm().isRemoteChatConfigured();
        boolean remoteEmbeddingEnabled = properties.getLlm().isRemoteEmbeddingConfigured();
        return new AppMetaResponse(
                properties.getStorage().getType(),
                properties.getRetrieval().getTopK(),
                properties.getMemory().getShortTermWindow(),
                remoteChatEnabled,
                remoteEmbeddingEnabled,
                remoteEmbeddingEnabled ? "remote" : "fallback",
                properties.getLlm().getChatModel(),
                properties.getLlm().getEmbeddingModel()
        );
    }

    public record AppMetaResponse(
            String storageType,
            int retrievalTopK,
            int shortTermWindow,
            boolean remoteChatEnabled,
            boolean remoteEmbeddingEnabled,
            String embeddingMode,
            String chatModel,
            String embeddingModel
    ) {
    }
}
