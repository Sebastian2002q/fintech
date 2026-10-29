package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import org.springframework.stereotype.Service;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;

@Service
public class SnapshotCoordinator {
    private static final int SCHEMA_VERSION = 1;
    private final SnapshotStore snapshotStore;
    private final SnapshotCodec codec;
    private final List<SnapshotParticipant<?>> participants;
    private final Map<String, SnapshotParticipant<?>> participantsByKey;

    public SnapshotCoordinator(SnapshotStore snapshotStore, JsonMapper mapper, List<SnapshotParticipant<?>> participants) {
        this.snapshotStore = Objects.requireNonNull(snapshotStore, "Snapshot store is required");
        this.codec = new SnapshotCodec(Objects.requireNonNull(mapper, "JsonMapper is required"));
        Objects.requireNonNull(participants, "Participants are required");
        this.participants = List.copyOf(participants);
        if (this.participants.isEmpty()) {
            throw new IllegalArgumentException("At least one participant is required");
        }
        this.participantsByKey = new HashMap<>();
        for (SnapshotParticipant<?> participant : this.participants) {
            Objects.requireNonNull(participant, "Participant cannot be null");
            String key = Objects.requireNonNull(participant.moduleKey(), "Module key is required");
            if (key.isBlank()) {
                throw new IllegalArgumentException("Module key cannot be blank");
            }
            Objects.requireNonNull(participant.stateType(), "State type is required");
            if (participantsByKey.putIfAbsent(key, participant) != null) {
                throw new IllegalArgumentException("Duplicate snapshot module: " + key);
            }
        }
    }

    public void save(long lastSequence) {
        if (lastSequence < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }
        Map<String, JsonNode> moduleStates = new HashMap<>();
        for (SnapshotParticipant<?> participant : participants) {
            moduleStates.put(participant.moduleKey(), captureState(participant));
        }
        StoredSnapshot snapshot = new StoredSnapshot(SCHEMA_VERSION, lastSequence, Instant.now(), moduleStates);
        snapshotStore.save(snapshot);
    }

    public OptionalLong restoreLatest() {
        Optional<StoredSnapshot> latest = snapshotStore.loadLatest();
        if (latest.isEmpty()) {
            return OptionalLong.empty();
        }
        StoredSnapshot snapshot = latest.get();
        if (snapshot.schemaVersion() != SCHEMA_VERSION) {
            throw new IllegalStateException("Unsupported snapshot schema version");
        }
        Map<String, JsonNode> moduleStates = snapshot.moduleStates();
        if (!moduleStates.keySet().equals(participantsByKey.keySet())) {
            return OptionalLong.empty();
        }
        List<Runnable> restoreOperations = new ArrayList<>();
        for (SnapshotParticipant<?> participant : participants) {
            JsonNode node = moduleStates.get(participant.moduleKey());
            restoreOperations.add(prepareRestore(participant, node));
        }
        for (Runnable operation : restoreOperations) {
            operation.run();
        }
        return OptionalLong.of(snapshot.lastSequence());
    }

    private <T> JsonNode captureState(SnapshotParticipant<T> participant) {
        return codec.encode(participant.capture());
    }

    private <T> Runnable prepareRestore(SnapshotParticipant<T> participant, JsonNode node) {
        T state = codec.decode(node, participant.stateType());
        return () -> participant.restore(state);
    }
}
