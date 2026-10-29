package co.edu.uniquindio.fintech_backend.persistence.recovery;

import co.edu.uniquindio.fintech_backend.persistence.event.EventReplayCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import org.springframework.stereotype.Service;

import java.util.Objects;

@Service
public class SystemRecoveryCoordinator {
    private final SnapshotCoordinator snapshotCoordinator;
    private final EventReplayCoordinator eventReplayCoordinator;

    public SystemRecoveryCoordinator(SnapshotCoordinator snapshotCoordinator, EventReplayCoordinator eventReplayCoordinator) {
        this.snapshotCoordinator = Objects.requireNonNull(snapshotCoordinator, "Snapshot coordinator is required");
        this.eventReplayCoordinator = Objects.requireNonNull(eventReplayCoordinator, "Event replay coordinator is required");
    }

    public long recover() {
        long lastSequence = snapshotCoordinator.restoreLatest().orElse(0L);
        return eventReplayCoordinator.replayAfter(lastSequence);
    }
}
