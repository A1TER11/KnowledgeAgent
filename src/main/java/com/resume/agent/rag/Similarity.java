package com.resume.agent.rag;

import java.util.List;

public final class Similarity {

    private Similarity() {
    }

    public static double cosine(List<Double> left, List<Double> right) {
        int size = Math.min(left.size(), right.size());
        double dot = 0;
        for (int i = 0; i < size; i++) {
            dot += left.get(i) * right.get(i);
        }
        return dot;
    }
}
