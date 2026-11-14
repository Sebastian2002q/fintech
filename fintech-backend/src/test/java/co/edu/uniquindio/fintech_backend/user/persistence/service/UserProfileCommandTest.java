package co.edu.uniquindio.fintech_backend.user.persistence.service;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserProfileCommandTest {
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final EventStore eventStore = mock(EventStore.class);
    private final EventWriter writer = new EventWriter(eventStore);
    private final SnapshotCoordinator snapshots = mock(SnapshotCoordinator.class);
    private final PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(eventStore, snapshots);
    private final UserCommandService service = new UserCommandService(repository, writer, operations);

    @Test
    void shouldUpdateProfileAndPersistEvent() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        // Act
        User updated = service.updateProfile(original.getId(), "Carlos Alberto", "Quintero", "  CARLOS@HOTMAIL.COM  ");
        // Assert
        assertEquals(original.getId(), updated.getId());
        assertEquals(original.getIdentification(), updated.getIdentification());
        assertEquals(original.getCreatedAt(), updated.getCreatedAt());
        assertEquals(original.getRole(), updated.getRole());
        assertEquals(original.getStatus(), updated.getStatus());
        assertEquals("Carlos Alberto", updated.getFirstName());
        assertEquals("Quintero", updated.getLastName());
        assertEquals("carlos@hotmail.com", updated.getEmail());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        assertEquals(original.getId(), repository.findByEmail("carlos@hotmail.com").orElseThrow().getId());
        verify(eventStore, times(2)).append(captor.capture());
        List<StoredEvent> events = captor.getAllValues();
        assertEquals(EventType.USER_UPDATED, events.get(1).type());
        User persisted = UserEventMapper.toUser(events.get(1).data());
        assertEquals(updated.getId(), persisted.getId());
        assertEquals(updated.getEmail(), persisted.getEmail());
    }

    @Test
    void shouldAllowUserToKeepCurrentEmail() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act
        User updated = service.updateProfile(original.getId(), "Carlos Alberto", "Perez", " CARLOS@GMAIL.COM ");
        // Assert
        assertEquals("Carlos Alberto", updated.getFirstName());
        assertEquals("carlos@gmail.com", updated.getEmail());
        assertEquals(1, repository.findAll().size());
        verify(eventStore, times(2)).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectEmailOwnedByAnotherUser() {
        // Arrange
        User first = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        service.register("1090654321", "Maria", "Gomez", "maria@gmail.com");
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> service.updateProfile(first.getId(), "Carlos", "Perez", " MARIA@GMAIL.COM "));
        assertEquals("carlos@gmail.com", repository.findById(first.getId()).orElseThrow().getEmail());
        assertEquals(2, repository.findAll().size());
        verify(eventStore, times(2)).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectUnknownUserWithoutWritingEvent() {
        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> service.updateProfile(UUID.randomUUID(), "Carlos", "Perez", "carlos@gmail.com"));
        assertTrue(repository.findAll().isEmpty());
        verify(eventStore, never()).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectInvalidProfileWithoutChangingStoredUser() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(original.getId(), "", "Perez", "carlos@gmail.com"));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(original.getId(), "Carlos", "", "carlos@gmail.com"));
        assertThrows(IllegalArgumentException.class, () -> service.updateProfile(original.getId(), "Carlos", "Perez", " "));
        User stored = repository.findById(original.getId()).orElseThrow();
        assertEquals("Carlos", stored.getFirstName());
        assertEquals("Perez", stored.getLastName());
        assertEquals("carlos@gmail.com", stored.getEmail());
        verify(eventStore, times(1)).append(any(StoredEvent.class));
    }

    @Test
    void shouldNotModifyRepositoryWhenEventWritingFails() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        doThrow(new IllegalStateException("Write failed")).when(eventStore).append(argThat(event -> event.type() == EventType.USER_UPDATED));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> service.updateProfile(original.getId(), "Carlos Alberto", "Quintero", "nuevo@gmail.com"));
        User stored = repository.findById(original.getId()).orElseThrow();
        assertEquals("Carlos", stored.getFirstName());
        assertEquals("Perez", stored.getLastName());
        assertEquals("carlos@gmail.com", stored.getEmail());
        assertTrue(repository.findByEmail("nuevo@gmail.com").isEmpty());
    }
}
