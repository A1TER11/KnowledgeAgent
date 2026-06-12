package com.resume.agent.config;

public enum StorageMode {
    MEMORY,
    POSTGRES;

    public static StorageMode from(String raw) {
        if (raw == null || raw.isBlank()) {
            return MEMORY;
        }
        return StorageMode.valueOf(raw.trim().toUpperCase());
    }
}
