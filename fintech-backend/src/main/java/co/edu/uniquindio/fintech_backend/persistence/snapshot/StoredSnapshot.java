package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import tools.jackson.databind.JsonNode;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public record StoredSnapshot(int schemaVersion, long lastSequence, Instant createdAt,
                             Map<String, JsonNode> moduleStates) {

    public StoredSnapshot {
        if (schemaVersion <= 0) {
            throw new IllegalArgumentException("Schema version must be positive");
        }
        if (lastSequence < 0) {
            throw new IllegalArgumentException("Last sequence cannot be negative");
        }
        Objects.requireNonNull(createdAt, "Creation date cannot be null");
        Objects.requireNonNull(moduleStates, "Module states cannot be null");
        moduleStates = copyStates(moduleStates);
    }

    @Override
    public Map<String, JsonNode> moduleStates() {
        return copyStates(moduleStates);
    }

    private static Map<String, JsonNode> copyStates(Map<String, JsonNode> states) {
        Map<String, JsonNode> copies = new HashMap<>();
        for (Map.Entry<String, JsonNode> entry : states.entrySet()) {
            String moduleName = entry.getKey();
            JsonNode state = entry.getValue();
            if (moduleName == null || moduleName.isBlank()) {
                throw new IllegalArgumentException("Module name cannot be blank");
            }
            Objects.requireNonNull(state, "Module state cannot be null");
            copies.put(moduleName, state.deepCopy());
        }
        return Map.copyOf(copies);
    }
}
