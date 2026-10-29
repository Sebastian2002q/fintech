package co.edu.uniquindio.fintech_backend.persistence.coordination;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.function.Supplier;

@Service
public class PersistenceOperationCoordinator {
    private final EventStore eventStore;
    private final SnapshotCoordinator snapshotCoordinator;
    private boolean recoveryRequired;

    public PersistenceOperationCoordinator(EventStore eventStore, SnapshotCoordinator snapshotCoordinator) {
        this.eventStore = Objects.requireNonNull(eventStore, "Event store is required");
        this.snapshotCoordinator = Objects.requireNonNull(snapshotCoordinator, "Snapshot coordinator is required");
    }

    public synchronized <T> T execute(Supplier<T> operation) {
        Objects.requireNonNull(operation, "Operation is required");
        ensureReady();
        long initialSequence = eventStore.getLastSequence();
        try {
            return operation.get();
        } catch (RuntimeException | Error failure) {
            try {
                if (eventStore.getLastSequence() != initialSequence) {
                    recoveryRequired = true;
                }
            } catch (RuntimeException | Error sequenceFailure) {
                recoveryRequired = true;
                failure.addSuppressed(sequenceFailure);
            }
            throw failure;
        }
    }

    public synchronized void saveSnapshot() {
        ensureReady();
        snapshotCoordinator.save(eventStore.getLastSequence());
    }

    private void ensureReady() {
        if (recoveryRequired) {
            throw new IllegalStateException("Persistence requires recovery");
        }
    }
}

