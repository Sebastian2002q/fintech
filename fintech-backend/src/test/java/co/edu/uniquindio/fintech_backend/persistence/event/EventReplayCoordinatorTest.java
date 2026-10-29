package co.edu.uniquindio.fintech_backend.persistence.event;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventHandler;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EventReplayCoordinatorTest {
    private final EventStore eventStore = mock(EventStore.class);
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final EventHandler userHandler = new UserEventHandler(repository);
    private final EventReplayCoordinator coordinator = new EventReplayCoordinator(eventStore, List.of(userHandler));

    private StoredEvent createEvent(long sequence, EventType type, User user) {
        return new StoredEvent(sequence, UUID.randomUUID(), type, Instant.now(), UserEventMapper.toData(user));
    }

    @Test
    void shouldReplayEventsInSequenceOrder() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User updated = new User(original);
        updated.updateProfile("Carlos Alberto", "Quintero", "nuevo@gmail.com");
        updated.changeRole(Role.ADMIN);
        updated.block();
        StoredEvent registered = createEvent(1, EventType.USER_REGISTERED, original);
        StoredEvent modified = createEvent(2, EventType.USER_UPDATED, updated);
        when(eventStore.readAfter(0)).thenReturn(List.of(registered, modified));
        // Act
        long lastSequence = coordinator.replayAfter(0);
        // Assert
        assertEquals(2, lastSequence);
        assertEquals(1, repository.findAll().size());
        User restored = repository.findById(original.getId()).orElseThrow();
        assertEquals("Carlos Alberto", restored.getFirstName());
        assertEquals("Quintero", restored.getLastName());
        assertEquals("nuevo@gmail.com", restored.getEmail());
        assertEquals(Role.ADMIN, restored.getRole());
        assertEquals(UserStatus.BLOCKED, restored.getStatus());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        assertEquals(original.getId(), repository.findByEmail("nuevo@gmail.com").orElseThrow().getId());
    }

    @Test
    void shouldReplayEventsAfterSnapshotSequence() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(original);
        User updated = new User(original);
        updated.deactivate();
        StoredEvent event = createEvent(6, EventType.USER_UPDATED, updated);
        when(eventStore.readAfter(5)).thenReturn(List.of(event));
        // Act
        long lastSequence = coordinator.replayAfter(5);
        // Assert
        assertEquals(6, lastSequence);
        assertEquals(UserStatus.INACTIVE, repository.findById(original.getId()).orElseThrow().getStatus());
        verify(eventStore).readAfter(5);
    }

    @Test
    void shouldReturnLastSequenceWhenNoEventsExist() {
        // Arrange
        when(eventStore.readAfter(5)).thenReturn(List.of());
        // Act
        long lastSequence = coordinator.replayAfter(5);
        // Assert
        assertEquals(5, lastSequence);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectSequenceGapBeforeApplyingEvents() {
        // Arrange
        User first = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User second = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        StoredEvent firstEvent = createEvent(1, EventType.USER_REGISTERED, first);
        StoredEvent secondEvent = createEvent(3, EventType.USER_REGISTERED, second);
        when(eventStore.readAfter(0)).thenReturn(List.of(firstEvent, secondEvent));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> coordinator.replayAfter(0));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectEventsOutOfOrder() {
        // Arrange
        User first = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User second = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        StoredEvent firstEvent = createEvent(1, EventType.USER_REGISTERED, first);
        StoredEvent secondEvent = createEvent(2, EventType.USER_REGISTERED, second);
        when(eventStore.readAfter(0)).thenReturn(List.of(secondEvent, firstEvent));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> coordinator.replayAfter(0));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectUnsupportedEventTypeBeforeApplyingEvents() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        EventType unsupported = Arrays.stream(EventType.values()).filter(type -> !userHandler.supportedTypes().contains(type)).findFirst().orElseThrow();
        StoredEvent registered = createEvent(1, EventType.USER_REGISTERED, user);
        StoredEvent unsupportedEvent = createEvent(2, unsupported, user);
        when(eventStore.readAfter(0)).thenReturn(List.of(registered, unsupportedEvent));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> coordinator.replayAfter(0));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectDuplicateEventHandlers() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new EventReplayCoordinator(eventStore, List.of(userHandler, userHandler)));
    }

    @Test
    void shouldRejectInvalidConstructorArguments() {
        // Arrange
        List<EventHandler> handlersWithNull = new ArrayList<>();
        handlersWithNull.add(null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new EventReplayCoordinator(null, List.of(userHandler)));
        assertThrows(NullPointerException.class, () -> new EventReplayCoordinator(eventStore, null));
        assertThrows(NullPointerException.class, () -> new EventReplayCoordinator(eventStore, handlersWithNull));
        assertThrows(IllegalArgumentException.class, () -> new EventReplayCoordinator(eventStore, List.of()));
    }

    @Test
    void shouldRejectNegativeSequenceWithoutReadingEvents() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coordinator.replayAfter(-1));
        verifyNoInteractions(eventStore);
    }
}
