package com.resume.agent.shared.store.jdbc;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JdbcVectorSupport {

    private JdbcVectorSupport() {
    }

    public static String toVectorLiteral(List<Double> values) {
        return "[" + values.stream()
                .map(value -> String.format(java.util.Locale.US, "%.8f", value))
                .collect(Collectors.joining(",")) + "]";
    }

    public static List<Double> parseVector(String raw) {
        String normalized = raw.replace("[", "").replace("]", "").trim();
        if (normalized.isEmpty()) {
            return List.of();
        }
        return Arrays.stream(normalized.split(","))
                .map(String::trim)
                .map(Double::parseDouble)
                .toList();
    }
}
