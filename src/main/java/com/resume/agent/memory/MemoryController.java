package com.resume.agent.memory;

import com.resume.agent.memory.api.MemoryResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/memories")
public class MemoryController {

    private final LongTermMemoryService longTermMemoryService;

    public MemoryController(LongTermMemoryService longTermMemoryService) {
        this.longTermMemoryService = longTermMemoryService;
    }

    @GetMapping("/{userId}")
    public MemoryResponse list(@PathVariable String userId) {
        return new MemoryResponse(
                userId,
                longTermMemoryService.listMemories(userId).stream()
                        .map(memory -> new MemoryResponse.MemoryItemView(
                                memory.memoryId(),
                                memory.memoryType().name(),
                                memory.content(),
                                memory.source(),
                                memory.createdAt()))
                        .toList()
        );
    }
}
