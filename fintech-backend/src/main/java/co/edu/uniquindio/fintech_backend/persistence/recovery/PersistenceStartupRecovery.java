package co.edu.uniquindio.fintech_backend.persistence.recovery;

import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class PersistenceStartupRecovery implements SmartInitializingSingleton {
    private final SystemRecoveryCoordinator recoveryCoordinator;
    private final EventStore eventStore;

    public PersistenceStartupRecovery(SystemRecoveryCoordinator recoveryCoordinator, EventStore eventStore) {
        this.recoveryCoordinator = Objects.requireNonNull(recoveryCoordinator, "Recovery coordinator is required");
        this.eventStore = Objects.requireNonNull(eventStore, "Event store is required");
    }

    @Override
    public void afterSingletonsInstantiated() {
        long recoveredSequence = recoveryCoordinator.recover();
        long storedSequence = eventStore.getLastSequence();
        if (recoveredSequence != storedSequence) {
            throw new IllegalStateException("Recovered sequence does not match event store sequence");
        }
    }
}
