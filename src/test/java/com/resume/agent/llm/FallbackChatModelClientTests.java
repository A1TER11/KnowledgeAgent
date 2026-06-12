package com.resume.agent.llm;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.resume.agent.agent.AgentDecision;
import com.resume.agent.rag.RetrievedChunk;
import java.util.List;
import org.junit.jupiter.api.Test;

class FallbackChatModelClientTests {

    private final FallbackChatModelClient client = new FallbackChatModelClient();

    @Test
    void shouldReturnStructuredFallbackWhenKnowledgeIsMissing() {
        ChatModelResult result = client.answer(new AgentDecision(
                "s-1",
                "u-1",
                "公司股权政策是什么？",
                List.of(),
                List.of(),
                List.of(),
                List.of()
        ));

        assertTrue(result.answer().contains("本地降级回答"));
        assertTrue(result.answer().contains("无法可靠作答"));
    }

    @Test
    void shouldReturnDirectOfficeHoursAnswerWhenSnippetContainsSchedule() {
        ChatModelResult result = client.answer(new AgentDecision(
                "s-2",
                "u-2",
                "什么时候下班？",
                List.of(),
                List.of(new RetrievedChunk("doc-1", "员工手册", "标准办公时间为周一至周五 09:30 至 18:30，午休时间为 12:30 至 13:30。", 1.0d)),
                List.of(),
                List.of()
        ));

        assertEquals("根据知识库，标准办公时间为周一至周五 09:30 至 18:30，午休时间为 12:30 至 13:30。", result.answer());
    }
}
