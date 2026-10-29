package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotEntry;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotState;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SnapshotCoordinatorTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SnapshotStore snapshotStore = mock(SnapshotStore.class);
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final UserSnapshotService participant = new UserSnapshotService(repository);
    private final SnapshotCoordinator coordinator = new SnapshotCoordinator(snapshotStore, mapper, List.of(participant));

    @Test
    void shouldSaveAllRegisteredModuleStates() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        user.changeRole(Role.ADMIN);
        repository.save(user);
        ArgumentCaptor<StoredSnapshot> captor = ArgumentCaptor.forClass(StoredSnapshot.class);
        // Act
        coordinator.save(5);
        // Assert
        verify(snapshotStore).save(captor.capture());
        StoredSnapshot saved = captor.getValue();
        assertEquals(1, saved.schemaVersion());
        assertEquals(5, saved.lastSequence());
        assertNotNull(saved.createdAt());
        assertEquals(1, saved.moduleStates().size());
        assertTrue(saved.moduleStates().containsKey("users"));
        UserSnapshotState state = new SnapshotCodec(mapper).decode(saved.moduleStates().get("users"), UserSnapshotState.class);
        assertEquals(1, state.users().size());
        assertEquals(user.getId(), state.users().get(0).id());
        assertEquals(Role.ADMIN, state.users().get(0).role());
    }

    @Test
    void shouldRestoreUsersAndReturnLastSequence() {
        // Arrange
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        UUID restoredId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-15T10:30:00Z");
        UserSnapshotEntry entry = new UserSnapshotEntry(restoredId, "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.ADMIN, UserStatus.BLOCKED, createdAt);
        UserSnapshotState state = new UserSnapshotState(List.of(entry));
        StoredSnapshot snapshot = new StoredSnapshot(1, 7, Instant.now(), Map.of("users", new SnapshotCodec(mapper).encode(state)));
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        // Act
        OptionalLong sequence = coordinator.restoreLatest();
        // Assert
        assertTrue(sequence.isPresent());
        assertEquals(7, sequence.getAsLong());
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isEmpty());
        User restored = repository.findById(restoredId).orElseThrow();
        assertEquals(Role.ADMIN, restored.getRole());
        assertEquals(UserStatus.BLOCKED, restored.getStatus());
        assertEquals(createdAt, restored.getCreatedAt());
        assertEquals(restoredId, repository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        assertEquals(restoredId, repository.findByIdentification("1090123456").orElseThrow().getId());
    }

    @Test
    void shouldReturnEmptyWhenNoSnapshotExists() {
        // Arrange
        when(snapshotStore.loadLatest()).thenReturn(Optional.empty());
        // Act
        OptionalLong result = coordinator.restoreLatest();
        // Assert
        assertTrue(result.isEmpty());
        verify(snapshotStore).loadLatest();
    }

    @Test
    void shouldIgnoreSnapshotWithMissingModuleWithoutChangingRepository() {
        // Arrange
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        StoredSnapshot snapshot = new StoredSnapshot(1, 5, Instant.now(), Map.of());
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        // Act
        OptionalLong result = coordinator.restoreLatest();
        // Assert
        assertTrue(result.isEmpty());
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
    }

    @Test
    void shouldRejectUnsupportedSchemaVersion() {
        // Arrange
        UserSnapshotState state = new UserSnapshotState(List.of());
        StoredSnapshot snapshot = new StoredSnapshot(2, 5, Instant.now(), Map.of("users", new SnapshotCodec(mapper).encode(state)));
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        // Act & Assert
        assertThrows(IllegalStateException.class, coordinator::restoreLatest);
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectDuplicateModuleKeys() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> new SnapshotCoordinator(snapshotStore, mapper, List.of(participant, participant)));
    }

    @Test
    void shouldRejectInvalidJsonBeforeRestoringUsers() {
        // Arrange
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        StoredSnapshot snapshot = new StoredSnapshot(1, 5, Instant.now(), Map.of("users", mapper.readTree("{\"users\":\"invalid\"}")));
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        // Act & Assert
        assertThrows(JacksonException.class, coordinator::restoreLatest);
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
    }

    @Test
    void shouldRejectNegativeSequenceWithoutSaving() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> coordinator.save(-1));
        verify(snapshotStore, never()).save(any());
    }

    @Test
    void shouldIgnoreSnapshotWithUnknownModuleWithoutChangingRepository() {
        // Arrange
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        UserSnapshotState state = new UserSnapshotState(List.of());
        StoredSnapshot snapshot = new StoredSnapshot(1, 5, Instant.now(), Map.of("users", new SnapshotCodec(mapper).encode(state), "wallets", mapper.valueToTree("old wallet data")));
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        // Act
        OptionalLong result = coordinator.restoreLatest();
        // Assert
        assertTrue(result.isEmpty());
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
    }

    @Test
    void shouldIgnoreLegacySnapshotWhenNewModuleIsRegistered() {
        // Arrange
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        UserSnapshotState state = new UserSnapshotState(List.of());
        StoredSnapshot snapshot = new StoredSnapshot(1, 5, Instant.now(), Map.of("users", new SnapshotCodec(mapper).encode(state)));
        when(snapshotStore.loadLatest()).thenReturn(Optional.of(snapshot));
        SnapshotParticipant<String> newParticipant = new SnapshotParticipant<>() {
            @Override
            public String moduleKey() {
                return "wallets";
            }

            @Override
            public Class<String> stateType() {
                return String.class;
            }

            @Override
            public String capture() {
                return "";
            }

            @Override
            public void restore(String state) {
                fail("Incompatible snapshot must not restore any module");
            }
        };
        SnapshotCoordinator expandedCoordinator = new SnapshotCoordinator(snapshotStore, mapper, List.of(participant, newParticipant));
        // Act
        OptionalLong result = expandedCoordinator.restoreLatest();
        // Assert
        assertTrue(result.isEmpty());
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
    }

}
