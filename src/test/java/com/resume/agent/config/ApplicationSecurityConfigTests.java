package com.resume.agent.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class ApplicationSecurityConfigTests {

    @Test
    void applicationConfigShouldUsePlaceholdersInsteadOfRealSecrets() throws IOException {
        String yaml = Files.readString(Path.of("src/main/resources/application.yml"), StandardCharsets.UTF_8);

        assertTrue(yaml.contains("${DEEPSEEK_API_KEY:replace-me}"));
        assertTrue(yaml.contains("${EMBEDDING_API_KEY:replace-me}"));
        assertTrue(yaml.contains("${MCP_SERVER_TOKEN:replace-me}"));
        assertTrue(yaml.contains("${AGENT_DB_PASSWORD:replace-me}"));

        assertFalse(yaml.contains("sk-"));
        assertFalse(yaml.contains("ghp_"));
    }
}
