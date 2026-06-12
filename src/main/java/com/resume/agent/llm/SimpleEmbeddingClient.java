package com.resume.agent.llm;

import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class SimpleEmbeddingClient implements EmbeddingClient {

    private static final int VECTOR_SIZE = 12;

    @Override
    public List<Double> embed(String text) {
        double[] vector = new double[VECTOR_SIZE];
        String normalized = text == null ? "" : text.toLowerCase();
        for (int i = 0; i < normalized.length(); i++) {
            int slot = i % VECTOR_SIZE;
            vector[slot] += normalized.charAt(i);
        }

        List<Double> result = new ArrayList<>(VECTOR_SIZE);
        double norm = 0;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm == 0 ? 1 : norm);
        for (double value : vector) {
            result.add(value / norm);
        }
        return result;
    }
}
