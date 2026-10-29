package co.edu.uniquindio.fintech_backend.persistence.event;

import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class EventWriter {
    private final EventStore eventStore;

    public EventWriter(EventStore eventStore) {
        this.eventStore = Objects.requireNonNull(eventStore, "Event store is required");
    }

    public synchronized StoredEvent record(EventType type, Map<String, String> data) {
        Objects.requireNonNull(type, "Event type is required");
        Objects.requireNonNull(data, "Event data is required");
        long sequence = eventStore.getLastSequence() + 1;
        StoredEvent event = new StoredEvent(sequence, UUID.randomUUID(), type, Instant.now(), data);
        eventStore.append(event);
        return event;
    }
}
