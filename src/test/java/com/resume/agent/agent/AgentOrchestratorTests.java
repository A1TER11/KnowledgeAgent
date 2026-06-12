package com.resume.agent.agent;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.resume.agent.memory.LongTermMemoryService;
import com.resume.agent.memory.ShortTermMemoryService;
import com.resume.agent.rag.RagService;
import com.resume.agent.shared.model.MemoryType;
import com.resume.agent.shared.store.ChatMessageStore;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AgentOrchestratorTests {

    @Mock
    private ChatMessageStore chatMessageStore;

    @Mock
    private ShortTermMemoryService shortTermMemoryService;

    @Mock
    private LongTermMemoryService longTermMemoryService;

    @Mock
    private RagService ragService;

    @Test
    void shouldExtractUserPreferenceMemoryFromShortMessage() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                chatMessageStore,
                shortTermMemoryService,
                longTermMemoryService,
                ragService
        );
        when(shortTermMemoryService.recentContext("s-1")).thenReturn(List.of());
        when(ragService.search("我喜欢先看结论")).thenReturn(List.of());
        when(longTermMemoryService.searchRelevant("u-1", "我喜欢先看结论", 3)).thenReturn(List.of());

        orchestrator.prepare("s-1", "u-1", "我喜欢先看结论");

        verify(longTermMemoryService).remember("u-1", MemoryType.USER_PREFERENCE, "我喜欢先看结论", "chat");
    }

    @Test
    void shouldExtractBusinessFactMemoryFromProjectContext() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                chatMessageStore,
                shortTermMemoryService,
                longTermMemoryService,
                ragService
        );
        when(shortTermMemoryService.recentContext("s-2")).thenReturn(List.of());
        when(ragService.search("我负责这个项目的前端演示")).thenReturn(List.of());
        when(longTermMemoryService.searchRelevant("u-2", "我负责这个项目的前端演示", 3)).thenReturn(List.of());

        orchestrator.prepare("s-2", "u-2", "我负责这个项目的前端演示");

        verify(longTermMemoryService).remember("u-2", MemoryType.BUSINESS_FACT, "我负责这个项目的前端演示", "chat");
    }

    @Test
    void shouldIgnoreLongOrStructuredMessagesWhenExtractingMemory() {
        AgentOrchestrator orchestrator = new AgentOrchestrator(
                chatMessageStore,
                shortTermMemoryService,
                longTermMemoryService,
                ragService
        );
        String message = """
                ## 项目总结
                我喜欢先看结论，再看分析。
                """;
        when(shortTermMemoryService.recentContext("s-3")).thenReturn(List.of());
        when(ragService.search(message)).thenReturn(List.of());
        when(longTermMemoryService.searchRelevant(eq("u-3"), eq(message), anyInt())).thenReturn(List.of());

        orchestrator.prepare("s-3", "u-3", message);

        verify(longTermMemoryService, never()).remember(eq("u-3"), eq(MemoryType.USER_PREFERENCE), eq(message), eq("chat"));
    }
}
