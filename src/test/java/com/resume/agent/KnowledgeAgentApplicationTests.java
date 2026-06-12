package com.resume.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "agent.storage.type=memory",
        "agent.llm.remote-chat-enabled=false",
        "agent.llm.remote-embedding-enabled=false",
        "agent.llm.local-embedding-fallback-enabled=true"
})
class KnowledgeAgentApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldUploadDocumentAndAnswerFromKnowledgeBase() throws Exception {
        String documentId = mockMvc.perform(post("/api/docs/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"员工手册",
                                  "content":"公司规定：知识库回答要优先基于文档，不允许编造。\\n\\n项目背景：Agent 项目需要支持 RAG、工具调用和记忆。"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("员工手册"))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"documentId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/docs/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("员工手册"));

        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId":"s-1",
                                  "userId":"u-1",
                                  "message":"这个 Agent 项目需要支持什么？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeSnippets").isNotEmpty());
    }

    @Test
    void shouldAnswerOfficeHoursQuestionFromSeededHandbook() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId":"s-office",
                                  "userId":"u-office",
                                  "message":"什么时候下班？"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.knowledgeSnippets").isNotEmpty())
                .andExpect(jsonPath("$.knowledgeSnippets[0].content").value(org.hamcrest.Matchers.containsString("18:30")));
    }

    @Test
    void shouldPersistLongTermMemoryAndExposeIt() throws Exception {
        mockMvc.perform(post("/api/chat")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sessionId":"s-2",
                                  "userId":"u-2",
                                  "message":"我喜欢回答时先给结论，再解释原因。"
                                }
                                """))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/memories/u-2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memories[0].memoryType").value("USER_PREFERENCE"));
    }

    @Test
    void shouldExposeWorkbenchAndMetaInfo() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/meta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.storageType").value("memory"))
                .andExpect(jsonPath("$.chatModel").value("deepseek-v4-pro"));
    }

    @Test
    void shouldSupportDocumentDetailUpdateAndDelete() throws Exception {
        String documentId = mockMvc.perform(post("/api/docs/upload")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"测试文档",
                                  "content":"第一段内容\\n\\n第二段内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.chunkCount").value(1))
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"documentId\":\"([^\"]+)\".*", "$1");

        mockMvc.perform(get("/api/docs/" + documentId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("测试文档"))
                .andExpect(jsonPath("$.chunkCount").value(1));

        mockMvc.perform(put("/api/docs/" + documentId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"测试文档-更新",
                                  "content":"只有一段更新后的内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("测试文档-更新"))
                .andExpect(jsonPath("$.chunkCount").value(1));

        mockMvc.perform(delete("/api/docs/" + documentId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/docs/" + documentId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldSeedExampleDocumentsWhenStoreIsEmpty() throws Exception {
        mockMvc.perform(get("/api/docs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.title=='Employee Handbook')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.title=='Expense Policy')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.title=='Project Status Report')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.title=='Technical Overview')]").isNotEmpty())
                .andExpect(jsonPath("$[?(@.title=='Project Tech Stack')]").isNotEmpty());
    }
}
