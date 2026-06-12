package com.resume.agent.chat.api;

import jakarta.validation.constraints.NotBlank;

public record ChatRequest(
        @NotBlank String sessionId,
        @NotBlank String userId,
        @NotBlank String message
) {
}
