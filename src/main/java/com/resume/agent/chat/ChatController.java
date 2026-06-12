package com.resume.agent.chat;

import com.resume.agent.chat.api.ChatRequest;
import com.resume.agent.chat.api.ChatResponse;
import com.resume.agent.chat.api.SessionResponse;
import com.resume.agent.chat.api.ToolCatalogResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatApplicationService chatApplicationService;

    public ChatController(ChatApplicationService chatApplicationService) {
        this.chatApplicationService = chatApplicationService;
    }

    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        return chatApplicationService.chat(request);
    }

    @GetMapping("/sessions/{sessionId}")
    public SessionResponse session(@PathVariable String sessionId) {
        return chatApplicationService.session(sessionId);
    }

    @GetMapping("/tools")
    public ToolCatalogResponse tools() {
        return chatApplicationService.toolCatalog();
    }
}
