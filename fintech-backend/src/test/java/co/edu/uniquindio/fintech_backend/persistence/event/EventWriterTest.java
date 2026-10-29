package co.edu.uniquindio.fintech_backend.persistence.event;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventWriterTest {
    private final EventStore eventStore = mock(EventStore.class);
    private final EventWriter writer = new EventWriter(eventStore);

    @Test
    void shouldRecordEventWithCorrectData() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(5L);
        Map<String, String> data = Map.of("id", "123", "name", "Carlos");
        Instant before = Instant.now();
        // Act
        StoredEvent event = writer.record(EventType.USER_REGISTERED, data);
        Instant after = Instant.now();
        // Assert
        assertEquals(6, event.sequence());
        assertEquals(EventType.USER_REGISTERED, event.type());
        assertEquals(data, event.data());
        assertNotNull(event.eventId());
        assertFalse(event.occurredAt().isBefore(before));
        assertFalse(event.occurredAt().isAfter(after));
        verify(eventStore).append(event);
    }

    @Test
    void shouldGenerateConsecutiveEventSequences() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(5L, 6L);
        // Act
        StoredEvent first = writer.record(EventType.USER_REGISTERED, Map.of("id", "1"));
        StoredEvent second = writer.record(EventType.USER_UPDATED, Map.of("id", "1"));
        // Assert
        assertEquals(6, first.sequence());
        assertEquals(7, second.sequence());
        assertNotEquals(first.eventId(), second.eventId());
        verify(eventStore, times(2)).getLastSequence();
        verify(eventStore, times(2)).append(any(StoredEvent.class));
    }

    @Test
    void shouldPreserveEventDataFromExternalModification() {
        // Arrange
        Map<String, String> data = new HashMap<>();
        data.put("name", "Carlos");
        // Act
        StoredEvent event = writer.record(EventType.USER_REGISTERED, data);
        data.put("name", "Maria");
        // Assert
        assertEquals("Carlos", event.data().get("name"));
        assertThrows(UnsupportedOperationException.class, () -> event.data().put("name", "Pedro"));
        verify(eventStore).append(event);
    }

    @Test
    void shouldRejectNullArgumentsWithoutWriting() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new EventWriter(null));
        assertThrows(NullPointerException.class, () -> writer.record(null, Map.of()));
        assertThrows(NullPointerException.class, () -> writer.record(EventType.USER_REGISTERED, null));
        verifyNoInteractions(eventStore);
    }

    @Test
    void shouldPropagateEventStoreFailures() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(5L);
        doThrow(new IllegalStateException("Write failed")).when(eventStore).append(any(StoredEvent.class));
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> writer.record(EventType.USER_REGISTERED, Map.of("id", "1")));
        assertEquals("Write failed", exception.getMessage());
        verify(eventStore).append(any(StoredEvent.class));
    }

    @Test
    void shouldUseLatestSequenceFromEventStore() {
        // Arrange
        when(eventStore.getLastSequence()).thenReturn(10L);
        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        // Act
        StoredEvent event = writer.record(EventType.USER_UPDATED, Map.of("id", "123"));
        // Assert
        verify(eventStore).append(captor.capture());
        assertEquals(11, captor.getValue().sequence());
        assertSame(event, captor.getValue());
    }
}
