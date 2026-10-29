package co.edu.uniquindio.fintech_backend.persistence.snapshot;

public interface SnapshotParticipant<T> {
    String moduleKey();
    Class<T> stateType();
    T capture();
    void restore(T state);
}
