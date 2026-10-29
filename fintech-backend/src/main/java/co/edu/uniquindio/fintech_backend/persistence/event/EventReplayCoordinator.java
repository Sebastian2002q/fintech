package co.edu.uniquindio.fintech_backend.persistence.event;

import org.springframework.stereotype.Service;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class EventReplayCoordinator {
    private final EventStore eventStore;
    private final Map<EventType, EventHandler> handlersByType;

    public EventReplayCoordinator(EventStore eventStore, List<EventHandler> handlers) {
        this.eventStore = Objects.requireNonNull(eventStore, "Event store is required");
        Objects.requireNonNull(handlers, "Event handlers are required");
        if (handlers.isEmpty()) {
            throw new IllegalArgumentException("At least one event handler is required");
        }
        this.handlersByType = new EnumMap<>(EventType.class);
        for (EventHandler handler : handlers) {
            Objects.requireNonNull(handler, "Event handler cannot be null");
            Set<EventType> types = Objects.requireNonNull(handler.supportedTypes(), "Supported types are required");
            if (types.isEmpty()) {
                throw new IllegalArgumentException("Event handler must support at least one type");
            }
            for (EventType type : types) {
                Objects.requireNonNull(type, "Event type cannot be null");
                if (handlersByType.putIfAbsent(type, handler) != null) {
                    throw new IllegalArgumentException("Duplicate event handler for: " + type);
                }
            }
        }
    }

    public long replayAfter(long lastSequence) {
        if (lastSequence < 0) {
            throw new IllegalArgumentException("Sequence cannot be negative");
        }
        List<StoredEvent> events = eventStore.readAfter(lastSequence);
        long expectedSequence = lastSequence;
        for (StoredEvent event : events) {
            Objects.requireNonNull(event, "Stored event cannot be null");
            if (event.sequence() != expectedSequence + 1) {
                throw new IllegalStateException("Invalid event sequence: " + event.sequence());
            }
            if (!handlersByType.containsKey(event.type())) {
                throw new IllegalStateException("No handler registered for: " + event.type());
            }
            expectedSequence = event.sequence();
        }
        for (StoredEvent event : events) {
            handlersByType.get(event.type()).apply(event);
        }
        return expectedSequence;
    }
}
