package com.resume.agent.docs;

import com.resume.agent.docs.api.DocumentDetailView;
import com.resume.agent.docs.api.DocumentUploadRequest;
import com.resume.agent.docs.api.DocumentView;
import com.resume.agent.rag.RagService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/docs")
public class DocumentController {

    private final RagService ragService;
    private final DemoKnowledgeSeeder demoKnowledgeSeeder;

    public DocumentController(RagService ragService, DemoKnowledgeSeeder demoKnowledgeSeeder) {
        this.ragService = ragService;
        this.demoKnowledgeSeeder = demoKnowledgeSeeder;
    }

    @PostMapping("/upload")
    public DocumentView upload(@Valid @RequestBody DocumentUploadRequest request) {
        return ragService.upload(request);
    }

    @GetMapping
    public List<DocumentView> listDocuments() {
        return ragService.listDocuments();
    }

    @PostMapping("/reseed-examples")
    public ExampleSeedResponse reseedExamples() {
        DemoKnowledgeSeeder.SeedResult result = demoKnowledgeSeeder.importMissingExampleDocuments();
        return new ExampleSeedResponse(result.importedCount(), result.examplesDirectoryMissing());
    }

    @GetMapping("/{documentId}")
    public DocumentDetailView document(@PathVariable String documentId) {
        return ragService.documentDetail(documentId);
    }

    @PutMapping("/{documentId}")
    public DocumentDetailView update(
            @PathVariable String documentId,
            @Valid @RequestBody DocumentUploadRequest request
    ) {
        return ragService.update(documentId, request);
    }

    @DeleteMapping("/{documentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable String documentId) {
        ragService.delete(documentId);
    }

    public record ExampleSeedResponse(int importedCount, boolean examplesDirectoryMissing) {
    }
}
