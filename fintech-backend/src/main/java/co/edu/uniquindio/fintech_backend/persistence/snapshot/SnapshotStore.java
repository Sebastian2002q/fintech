package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import java.util.Optional;

public interface SnapshotStore {
    void save(StoredSnapshot snapshot);
    Optional<StoredSnapshot> loadLatest();
}
