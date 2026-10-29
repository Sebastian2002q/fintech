package co.edu.uniquindio.fintech_backend.persistence.event;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JsonlEventStoreTest {
    @TempDir
    Path tempDir;
    private JsonlEventStore eventStore;
    private Path filePath;
    private JsonMapper jsonMapper;

    @BeforeEach
    void setUp() {
        filePath = tempDir.resolve("events.jsonl");
        jsonMapper = JsonMapper.builder().build();
        eventStore = new JsonlEventStore(filePath.toString(), jsonMapper);
    }

    @AfterEach
    void tearDown() {
        if (eventStore != null) {
            eventStore.close();
        }
    }

    @Test
    void shouldAppendAndReadEvent() {
        // Arrange
        StoredEvent event = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("userId", UUID.randomUUID().toString(), "firstName", "Carlos", "email", "carlos@gmail.com"));
        // Act
        eventStore.append(event);
        List<StoredEvent> events = eventStore.readAfter(0);
        // Assert
        assertTrue(Files.exists(filePath));
        assertEquals(1, events.size());
        assertEquals(event, events.getFirst());
        assertEquals(1L, eventStore.getLastSequence());
    }

    @Test
    void shouldRecoverEventsAfterRestart() {
        // Arrange
        StoredEvent event1 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("userId", UUID.randomUUID().toString(), "firstName", "Carlos"));
        StoredEvent event2 = new StoredEvent(2L, UUID.randomUUID(), EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos Alberto"));
        eventStore.append(event1);
        eventStore.append(event2);
        // Simulate application shutdown
        eventStore.close();
        // Act
        try (JsonlEventStore recoveredStore = new JsonlEventStore(filePath.toString(), jsonMapper)) {
            List<StoredEvent> recoveredEvents = recoveredStore.readAfter(0);
            // Assert
            assertEquals(2, recoveredEvents.size());
            assertEquals(event1, recoveredEvents.get(0));
            assertEquals(event2, recoveredEvents.get(1));
            assertEquals(2L, recoveredStore.getLastSequence());
        }
    }

    @Test
    void shouldRejectDuplicateSequence() {
        // Arrange
        StoredEvent event1 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("userId", UUID.randomUUID().toString()));
        StoredEvent event2 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos"));
        eventStore.append(event1);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> eventStore.append(event2));
        assertEquals(1L, eventStore.getLastSequence());
        List<StoredEvent> events = eventStore.readAfter(0);
        assertEquals(1, events.size());
        assertEquals(event1, events.getFirst());
    }

    @Test
    void shouldRejectOutOfOrderSequence() {
        // Arrange
        StoredEvent event1 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("userId", UUID.randomUUID().toString()));
        StoredEvent event3 = new StoredEvent(3L, UUID.randomUUID(), EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos"));
        eventStore.append(event1);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> eventStore.append(event3));
        assertEquals(1L, eventStore.getLastSequence());
        List<StoredEvent> events = eventStore.readAfter(0);
        assertEquals(1, events.size());
        assertEquals(event1, events.getFirst());
    }

    @Test
    void shouldRejectIncompleteEventLog() throws Exception {
        // Arrange
        StoredEvent event = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("userId", UUID.randomUUID().toString(), "firstName", "Carlos"));
        eventStore.append(event);
        // Simulate an interrupted write
        Files.writeString(filePath, "{\"sequence\":2,\"type\":\"USER_UPD", StandardOpenOption.APPEND);
        // Close the original store before recovery
        eventStore.close();
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JsonlEventStore(filePath.toString(), jsonMapper));
    }

    @Test
    void shouldRejectDuplicateEventId() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        StoredEvent event1 = new StoredEvent(1L, eventId, EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        StoredEvent event2 = new StoredEvent(2L, eventId, EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos Alberto"));
        eventStore.append(event1);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> eventStore.append(event2));
        assertEquals(1L, eventStore.getLastSequence());
        List<StoredEvent> events = eventStore.readAfter(0);
        assertEquals(1, events.size());
        assertEquals(event1, events.getFirst());
    }

    @Test
    void shouldRejectDuplicateEventIdDuringRecovery() throws Exception {
        // Arrange
        UUID eventId = UUID.randomUUID();
        StoredEvent event1 = new StoredEvent(1L, eventId, EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        StoredEvent event2 = new StoredEvent(2L, eventId, EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos Alberto"));
        eventStore.append(event1);
        // Simulate a corrupted event log
        Files.writeString(filePath, jsonMapper.writeValueAsString(event2) + "\n", StandardOpenOption.APPEND);
        // Close the original store before recovery
        eventStore.close();
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JsonlEventStore(filePath.toString(), jsonMapper));
    }

    @Test
    void shouldRejectSecondWriterForSameFile() {
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> new JsonlEventStore(filePath.toString(), jsonMapper));
        // The original store must remain usable
        StoredEvent event = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        eventStore.append(event);
        assertEquals(1L, eventStore.getLastSequence());
    }

    @Test
    void shouldAllowNewWriterAfterClosingPreviousStore() {
        // Arrange
        StoredEvent event1 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        StoredEvent event2 = new StoredEvent(2L, UUID.randomUUID(), EventType.USER_UPDATED, Instant.now(), Map.of("firstName", "Carlos Alberto"));
        eventStore.append(event1);
        eventStore.append(event2);
        // Act
        eventStore.close();
        try (JsonlEventStore newStore = new JsonlEventStore(filePath.toString(), jsonMapper)) {
            assertEquals(2L, newStore.getLastSequence());
            StoredEvent event3 = new StoredEvent(3L, UUID.randomUUID(), EventType.USER_BLOCKED, Instant.now(), Map.of("reason", "Administrative action"));
            newStore.append(event3);
            // Assert
            List<StoredEvent> events = newStore.readAfter(0);
            assertEquals(3, events.size());
            assertEquals(event1, events.get(0));
            assertEquals(event2, events.get(1));
            assertEquals(event3, events.get(2));
            assertEquals(3L, newStore.getLastSequence());
        }
    }

    @Test
    void shouldRejectMalformedJsonDuringRecovery() throws Exception {
        // Arrange
        StoredEvent event = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        eventStore.append(event);
        // Simulate corrupted JSON with a valid line ending
        Files.writeString(filePath, "{\"sequence\":2,\"type\":\"USER_UPDATED\"\n", StandardOpenOption.APPEND);
        eventStore.close();
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JsonlEventStore(filePath.toString(), jsonMapper));
        assertTrue(exception.getMessage().contains("Invalid JSON at event sequence 2"));
        assertInstanceOf(JacksonException.class, exception.getCause());
    }

    @Test
    void shouldRejectSequenceGapDuringRecovery() throws Exception {
        // Arrange
        StoredEvent event1 = new StoredEvent(1L, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of("firstName", "Carlos"));
        StoredEvent event3 = new StoredEvent(3L, UUID.randomUUID(), EventType.USER_BLOCKED, Instant.now(), Map.of("reason", "Administrative action"));
        eventStore.append(event1);
        // Simulate an inconsistent event log
        Files.writeString(filePath, jsonMapper.writeValueAsString(event3) + "\n", StandardOpenOption.APPEND);
        // Simulate application shutdown
        eventStore.close();
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> new JsonlEventStore(filePath.toString(), jsonMapper));
        assertEquals("Invalid event sequence in file", exception.getMessage());
    }


}

