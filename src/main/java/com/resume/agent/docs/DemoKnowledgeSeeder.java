package com.resume.agent.docs;

import com.resume.agent.rag.RagService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DemoKnowledgeSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoKnowledgeSeeder.class);
    private static final String EXAMPLES_DIR = "examples";
    private static final String SKIPPED_FILE = "demo-prompts.txt";

    private final RagService ragService;

    public DemoKnowledgeSeeder(RagService ragService) {
        this.ragService = ragService;
    }

    @Override
    public void run(String... args) {
        importMissingExampleDocuments();
    }

    public SeedResult importMissingExampleDocuments() {
        Path examplesPath = Path.of(EXAMPLES_DIR);
        if (!Files.exists(examplesPath)) {
            log.warn("Knowledge seeding skipped because '{}' directory does not exist.", EXAMPLES_DIR);
            return new SeedResult(0, true);
        }

        try {
            List<Path> seedFiles = Files.list(examplesPath)
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().endsWith(".txt"))
                    .filter(path -> !path.getFileName().toString().equals(SKIPPED_FILE))
                    .sorted(Comparator.comparing(path -> path.getFileName().toString()))
                    .toList();

            int imported = 0;
            for (Path seedFile : seedFiles) {
                String content = Files.readString(seedFile, StandardCharsets.UTF_8).trim();
                if (content.isEmpty()) {
                    log.warn("Knowledge seeding skipped empty file '{}'.", seedFile.getFileName());
                    continue;
                }

                String title = toFriendlyTitle(seedFile);
                if (ragService.hasDocumentWithTitle(title)) {
                    log.info("Knowledge seeding skipped existing title '{}'.", title);
                    continue;
                }

                ragService.uploadSeed(title, content);
                imported++;
            }

            log.info("Knowledge seeding completed, imported {} missing example documents.", imported);
            return new SeedResult(imported, false);
        } catch (IOException exception) {
            log.warn("Knowledge seeding failed: {}", exception.getMessage());
            return new SeedResult(0, false);
        }
    }

    private String toFriendlyTitle(Path seedFile) {
        String raw = seedFile.getFileName().toString().replace(".txt", "");
        return String.join(" ", Arrays.stream(raw.split("-"))
                .map(token -> token.isBlank()
                        ? token
                        : token.substring(0, 1).toUpperCase(Locale.ROOT) + token.substring(1))
                .toList());
    }

    public record SeedResult(int importedCount, boolean examplesDirectoryMissing) {
    }
}
