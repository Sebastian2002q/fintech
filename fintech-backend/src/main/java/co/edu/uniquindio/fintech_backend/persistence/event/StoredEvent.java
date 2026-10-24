package co.edu.uniquindio.fintech_backend.persistence.event;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public record StoredEvent(long sequence, UUID eventId, EventType type, Instant occurredAt, Map<String, String> data) {

    public StoredEvent {
        if (sequence <= 0) {
            throw new IllegalArgumentException("Sequence must be positive");
        }
        Objects.requireNonNull(eventId, "Event ID cannot be null");
        Objects.requireNonNull(type, "Event type cannot be null");
        Objects.requireNonNull(occurredAt, "Event date cannot be null");
        Objects.requireNonNull(data, "Event data cannot be null");
        data = Map.copyOf(data);
    }
}
