package co.edu.uniquindio.fintech_backend.user.persistence.service;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventStore;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserCommandServiceTest {
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final EventStore eventStore = mock(EventStore.class);
    private final EventWriter eventWriter = new EventWriter(eventStore);
    private final SnapshotCoordinator snapshotCoordinator = mock(SnapshotCoordinator.class);
    private final PersistenceOperationCoordinator persistenceCoordinator = new PersistenceOperationCoordinator(eventStore, snapshotCoordinator);
    private final UserCommandService service = new UserCommandService(repository, eventWriter, persistenceCoordinator);

    @Test
    void shouldRegisterUserAndPersistEvent() {
        // Arrange
        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        // Act
        User user = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Assert
        verify(eventStore).append(captor.capture());
        StoredEvent event = captor.getValue();
        assertEquals(1, event.sequence());
        assertEquals(EventType.USER_REGISTERED, event.type());
        assertEquals(user.getId().toString(), event.data().get("id"));
        assertEquals("1090123456", event.data().get("identification"));
        assertEquals("carlos@gmail.com", event.data().get("email"));
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(1, repository.findAll().size());
        assertEquals(user.getId(), repository.findById(user.getId()).orElseThrow().getId());
        assertEquals(user.getId(), repository.findByIdentification("1090123456").orElseThrow().getId());
        assertEquals(user.getId(), repository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        verifyNoInteractions(snapshotCoordinator);
    }

    @Test
    void shouldNormalizeEmailBeforeRegisteringEvent() {
        // Arrange
        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        // Act
        User user = service.register("1090123456", "Carlos", "Perez", "  CARLOS@GMAIL.COM  ");
        // Assert
        verify(eventStore).append(captor.capture());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals("carlos@gmail.com", captor.getValue().data().get("email"));
        assertTrue(repository.findByEmail("carlos@gmail.com").isPresent());
    }

    @Test
    void shouldRejectDuplicateIdentificationWithoutWritingEvent() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.register("1090123456", "Maria", "Gomez", "maria@gmail.com"));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        verify(eventStore, times(1)).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectDuplicateEmailWithoutWritingEvent() {
        // Arrange
        User original = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.register("1090654321", "Maria", "Gomez", "  CARLOS@GMAIL.COM  "));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        assertTrue(repository.findByIdentification("1090654321").isEmpty());
        verify(eventStore, times(1)).append(any(StoredEvent.class));
    }

    @Test
    void shouldNotSaveUserWhenEventWritingFails() {
        // Arrange
        doThrow(new IllegalStateException("Event write failed")).when(eventStore).append(any(StoredEvent.class));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com"));
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        verify(eventStore).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectInvalidUserDataWithoutWritingEvent() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.register("", "Carlos", "Perez", "carlos@gmail.com"));
        assertThrows(IllegalArgumentException.class, () -> service.register("1090123456", "", "Perez", "carlos@gmail.com"));
        assertThrows(IllegalArgumentException.class, () -> service.register("1090123456", "Carlos", "Perez", "   "));
        assertTrue(repository.findAll().isEmpty());
        verify(eventStore, never()).append(any(StoredEvent.class));
        verifyNoInteractions(snapshotCoordinator);
    }

    @Test
    void shouldRejectNullDependencies() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new UserCommandService(null, eventWriter, persistenceCoordinator));
        assertThrows(NullPointerException.class, () -> new UserCommandService(repository, null, persistenceCoordinator));
        assertThrows(NullPointerException.class, () -> new UserCommandService(repository, eventWriter, null));
    }
}
