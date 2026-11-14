package co.edu.uniquindio.fintech_backend.auth.persistence.service;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.auth.service.PasswordHasher;
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
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AuthCommandServiceTest {
    private static final String PASSWORD = "MySecurePassword123!";
    private static final String HASH = "{bcrypt}encoded-test-hash";
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final EventStore store = mock(EventStore.class);
    private final EventWriter writer = new EventWriter(store);
    private final SnapshotCoordinator snapshots = mock(SnapshotCoordinator.class);
    private final PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(store, snapshots);
    private final AuthCommandService service = new AuthCommandService(users, credentials, hasher, writer, operations);

    private User register() {
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        return service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", PASSWORD);
    }

    @Test
    void shouldRegisterCompleteAccountWithSingleEvent() {
        // Act
        User user = register();
        // Assert
        ArgumentCaptor<StoredEvent> captor = ArgumentCaptor.forClass(StoredEvent.class);
        verify(store).append(captor.capture());
        StoredEvent event = captor.getValue();
        assertEquals(EventType.ACCOUNT_REGISTERED, event.type());
        assertEquals(1, event.sequence());
        assertEquals(user.getId().toString(), event.data().get("id"));
        assertEquals(HASH, event.data().get("passwordHash"));
        assertFalse(event.data().containsValue(PASSWORD));
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertEquals(user.getId(), users.findById(user.getId()).orElseThrow().getId());
        assertEquals(HASH, credentials.findByUserId(user.getId()).orElseThrow().passwordHash());
    }

    @Test
    void shouldNormalizeEmailAndPreserveIdentity() {
        // Arrange
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        // Act
        User user = service.register("1090123456", "Carlos", "Perez", " CARLOS@GMAIL.COM ", PASSWORD);
        // Assert
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(user.getId(), users.findByEmail("carlos@gmail.com").orElseThrow().getId());
        assertEquals(user.getId(), credentials.findAll().iterator().next().userId());
    }

    @Test
    void shouldRejectDuplicateIdentificationWithoutNewEvent() {
        // Arrange
        users.save(new User("1090123456", "Existing", "User", "existing@gmail.com"));
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", PASSWORD));
        assertTrue(credentials.findAll().isEmpty());
        verify(store, never()).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectDuplicateEmailWithoutNewEvent() {
        // Arrange
        users.save(new User("1090999999", "Existing", "User", "carlos@gmail.com"));
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        // Act & Assert
        assertThrows(UserAlreadyExistsException.class, () -> service.register("1090123456", "Carlos", "Perez", " CARLOS@GMAIL.COM ", PASSWORD));
        assertTrue(credentials.findAll().isEmpty());
        verify(store, never()).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectInvalidProfileOrPasswordWithoutWriting() {
        // Arrange
        when(hasher.hash("short")).thenThrow(new IllegalArgumentException("Invalid password"));
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> service.register("", "Carlos", "Perez", "carlos@gmail.com", PASSWORD));
        assertThrows(IllegalArgumentException.class, () -> service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", "short"));
        assertTrue(users.findAll().isEmpty());
        assertTrue(credentials.findAll().isEmpty());
        verify(store, never()).append(any(StoredEvent.class));
    }

    @Test
    void shouldNotSaveAccountWhenEventWritingFails() {
        // Arrange
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        doThrow(new IllegalStateException("Write failed")).when(store).append(any(StoredEvent.class));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", PASSWORD));
        assertTrue(users.findAll().isEmpty());
        assertTrue(credentials.findAll().isEmpty());
    }

    @Test
    void shouldBlockOperationsWhenCredentialStorageFailsAfterEvent() {
        // Arrange
        CredentialRepository failingCredentials = mock(CredentialRepository.class);
        when(hasher.hash(PASSWORD)).thenReturn(HASH);
        when(store.getLastSequence()).thenReturn(0L, 0L, 1L);
        doThrow(new IllegalStateException("Repository failed")).when(failingCredentials).save(any(Credential.class));
        AuthCommandService failingService = new AuthCommandService(users, failingCredentials, hasher, writer, operations);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> failingService.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", PASSWORD));
        assertEquals(1, users.findAll().size());
        IllegalStateException blocked = assertThrows(IllegalStateException.class, () -> failingService.register("1090654321", "Maria", "Gomez", "maria@gmail.com", PASSWORD));
        assertEquals("Persistence requires recovery", blocked.getMessage());
        verify(store, times(1)).append(any(StoredEvent.class));
    }

    @Test
    void shouldRejectNullDependencies() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new AuthCommandService(null, credentials, hasher, writer, operations));
        assertThrows(NullPointerException.class, () -> new AuthCommandService(users, null, hasher, writer, operations));
        assertThrows(NullPointerException.class, () -> new AuthCommandService(users, credentials, null, writer, operations));
        assertThrows(NullPointerException.class, () -> new AuthCommandService(users, credentials, hasher, null, operations));
        assertThrows(NullPointerException.class, () -> new AuthCommandService(users, credentials, hasher, writer, null));
    }
}
