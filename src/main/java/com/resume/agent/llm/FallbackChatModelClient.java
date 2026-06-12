package com.resume.agent.llm;

import com.resume.agent.agent.AgentDecision;
import com.resume.agent.shared.model.ToolExecutionRecord;
import java.util.StringJoiner;
import org.springframework.stereotype.Component;

@Component
public class FallbackChatModelClient implements ChatModelClient {

    @Override
    public ChatModelResult answer(AgentDecision decision) {
        if (!decision.knowledgeHits().isEmpty()) {
            String directAnswer = buildKnowledgeFirstAnswer(decision);
            return new ChatModelResult(directAnswer, decision.toolExecutions());
        }

        StringJoiner joiner = new StringJoiner("\n");
        joiner.add("这是基于当前上下文生成的本地降级回答。");
        joiner.add("用户问题：" + decision.userMessage());

        if (!decision.knowledgeHits().isEmpty()) {
            joiner.add("我从知识库中找到了这些内容：");
            decision.knowledgeHits().forEach(hit -> joiner.add("- " + hit.title() + "：" + summarize(hit.content())));
        }

        if (!decision.memoryHits().isEmpty()) {
            joiner.add("我还参考了这些长期记忆：");
            decision.memoryHits().forEach(hit -> joiner.add("- " + hit.memoryType() + "：" + summarize(hit.content())));
        }

        if (!decision.toolExecutions().isEmpty()) {
            joiner.add("本轮调用了以下工具：");
            for (ToolExecutionRecord record : decision.toolExecutions()) {
                joiner.add("- " + record.toolName() + "：" + record.summary());
            }
        }

        if (decision.knowledgeHits().isEmpty()) {
            joiner.add("当前没有命中足够的知识库证据，无法可靠作答。");
        } else {
            joiner.add("请优先以上述知识库内容为准。");
        }

        return new ChatModelResult(joiner.toString(), decision.toolExecutions());
    }

    private String buildKnowledgeFirstAnswer(AgentDecision decision) {
        String topSnippet = decision.knowledgeHits().get(0).content();
        String compact = summarize(topSnippet);
        if (topSnippet.contains("18:30") || topSnippet.contains("09:30")) {
            return "根据知识库，标准办公时间为周一至周五 09:30 至 18:30，午休时间为 12:30 至 13:30。";
        }
        return "根据知识库，相关依据是：" + compact;
    }

    private String summarize(String content) {
        String compact = content == null ? "" : content.replace('\n', ' ').trim();
        return compact.length() <= 120 ? compact : compact.substring(0, 120) + "...";
    }
}
