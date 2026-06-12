package com.resume.agent.llm;

import com.resume.agent.agent.AgentDecision;

public interface ChatModelClient {
    ChatModelResult answer(AgentDecision decision);
}
