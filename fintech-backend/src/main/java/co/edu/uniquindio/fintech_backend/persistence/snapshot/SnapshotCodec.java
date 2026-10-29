package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.util.Objects;

public final class SnapshotCodec {
    private final JsonMapper mapper;

    public SnapshotCodec(JsonMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "JsonMapper is required");
    }

    public JsonNode encode(Object state) {
        Objects.requireNonNull(state, "Snapshot state is required");
        return mapper.valueToTree(state);
    }

    public <T> T decode(JsonNode node, Class<T> type) {
        Objects.requireNonNull(node, "Snapshot node is required");
        Objects.requireNonNull(type, "Snapshot type is required");
        return mapper.treeToValue(node, type);
    }
}
