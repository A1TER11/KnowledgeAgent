package com.resume.agent.llm;

import java.util.List;

public interface EmbeddingClient {
    List<Double> embed(String text);
}
