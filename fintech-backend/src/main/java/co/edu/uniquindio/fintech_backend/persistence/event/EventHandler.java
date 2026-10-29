package co.edu.uniquindio.fintech_backend.persistence.event;

import java.util.Set;

public interface EventHandler {
    Set<EventType> supportedTypes();
    void apply(StoredEvent event);
}
