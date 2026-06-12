package com.resume.agent.docs.api;

import jakarta.validation.constraints.NotBlank;

public record DocumentUploadRequest(
        @NotBlank String title,
        @NotBlank String content
) {
}
