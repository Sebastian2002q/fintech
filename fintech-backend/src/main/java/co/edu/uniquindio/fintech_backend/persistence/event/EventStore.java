package co.edu.uniquindio.fintech_backend.persistence.event;

import java.util.List;

public interface EventStore {
    void append(StoredEvent event);
    List<StoredEvent> readAfter(long sequence);
    long getLastSequence();
}
