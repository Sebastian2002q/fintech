package co.edu.uniquindio.fintech_backend.persistence.recovery;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventReplayCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.persistence.event.JsonlEventStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.JsonSnapshotStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotStore;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventHandler;
import co.edu.uniquindio.fintech_backend.user.persistence.service.UserCommandService;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserPersistenceIntegrationTest {
    @TempDir
    Path tempDir;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private JsonlEventStore openStore(Path path) {
        return new JsonlEventStore(path.toString(), mapper);
    }

    @Test
    void shouldRecoverRegisteredUserAfterRestart() {
        // Arrange
        Path eventsPath = tempDir.resolve("events.jsonl");
        UUID originalId;
        User originalUser;
        // Register and persist the user
        try (JsonlEventStore firstStore = openStore(eventsPath)) {
            InMemoryUserRepository repository = new InMemoryUserRepository();
            EventWriter writer = new EventWriter(firstStore);
            SnapshotCoordinator snapshots = mock(SnapshotCoordinator.class);
            PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(firstStore, snapshots);
            UserCommandService commands = new UserCommandService(repository, writer, operations);
            originalUser = commands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
            originalId = originalUser.getId();
            assertEquals(1, firstStore.getLastSequence());
            assertEquals(1, repository.findAll().size());
        }
        // Simulate a restart with a new repository and event store
        try (JsonlEventStore secondStore = openStore(eventsPath)) {
            InMemoryUserRepository restoredRepository = new InMemoryUserRepository();
            UserSnapshotService participant = new UserSnapshotService(restoredRepository);
            SnapshotStore snapshotStore = mock(SnapshotStore.class);
            when(snapshotStore.loadLatest()).thenReturn(Optional.empty());
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(participant));
            EventReplayCoordinator replay = new EventReplayCoordinator(secondStore, List.of(new UserEventHandler(restoredRepository)));
            SystemRecoveryCoordinator recovery = new SystemRecoveryCoordinator(snapshots, replay);
            // Act
            long lastSequence = recovery.recover();
            // Assert
            assertEquals(1, lastSequence);
            assertEquals(1, restoredRepository.findAll().size());
            User restored = restoredRepository.findById(originalId).orElseThrow();
            assertEquals(originalUser.getId(), restored.getId());
            assertEquals(originalUser.getIdentification(), restored.getIdentification());
            assertEquals(originalUser.getFirstName(), restored.getFirstName());
            assertEquals(originalUser.getLastName(), restored.getLastName());
            assertEquals(originalUser.getEmail(), restored.getEmail());
            assertEquals(originalUser.getRole(), restored.getRole());
            assertEquals(originalUser.getStatus(), restored.getStatus());
            assertEquals(originalUser.getCreatedAt(), restored.getCreatedAt());
            assertEquals(originalId, restoredRepository.findByIdentification("1090123456").orElseThrow().getId());
            assertEquals(originalId, restoredRepository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        }
    }

    @Test
    void shouldRecoverUsersFromSnapshotAndSubsequentEvents() {
        // Arrange
        Path eventsPath = tempDir.resolve("events.jsonl");
        Path snapshotPath = tempDir.resolve("snapshot.json");
        UUID firstId;
        UUID secondId;
        // Register users and persist a snapshot
        try (JsonlEventStore firstStore = openStore(eventsPath)) {
            InMemoryUserRepository repository = new InMemoryUserRepository();
            SnapshotStore snapshotStore = new JsonSnapshotStore(snapshotPath.toString(), mapper);
            UserSnapshotService participant = new UserSnapshotService(repository);
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(participant));
            PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(firstStore, snapshots);
            UserCommandService commands = new UserCommandService(repository, new EventWriter(firstStore), operations);
            User firstUser = commands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com");
            firstId = firstUser.getId();
            operations.saveSnapshot();
            User secondUser = commands.register("1090654321", "Maria", "Gomez", "maria@gmail.com");
            secondId = secondUser.getId();
            assertEquals(2, firstStore.getLastSequence());
            assertEquals(2, repository.findAll().size());
        }
        // Simulate application restart
        try (JsonlEventStore secondStore = openStore(eventsPath)) {
            InMemoryUserRepository restoredRepository = new InMemoryUserRepository();
            SnapshotStore snapshotStore = new JsonSnapshotStore(snapshotPath.toString(), mapper);
            UserSnapshotService participant = new UserSnapshotService(restoredRepository);
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(participant));
            EventReplayCoordinator replay = new EventReplayCoordinator(secondStore, List.of(new UserEventHandler(restoredRepository)));
            SystemRecoveryCoordinator recovery = new SystemRecoveryCoordinator(snapshots, replay);
            // Act
            long lastSequence = recovery.recover();
            // Assert
            assertEquals(2, lastSequence);
            assertEquals(2, restoredRepository.findAll().size());
            User firstRestored = restoredRepository.findById(firstId).orElseThrow();
            User secondRestored = restoredRepository.findById(secondId).orElseThrow();
            assertEquals("Carlos", firstRestored.getFirstName());
            assertEquals("Maria", secondRestored.getFirstName());
            assertEquals(firstId, restoredRepository.findByIdentification("1090123456").orElseThrow().getId());
            assertEquals(secondId, restoredRepository.findByIdentification("1090654321").orElseThrow().getId());
            assertEquals(firstId, restoredRepository.findByEmail("carlos@gmail.com").orElseThrow().getId());
            assertEquals(secondId, restoredRepository.findByEmail("maria@gmail.com").orElseThrow().getId());
            assertEquals(1, snapshotStore.loadLatest().orElseThrow().lastSequence());
        }
    }

    @Test
    void shouldStopWritingAfterRepositoryFailureAndRecoverPersistedEvent() {
        // Arrange
        Path eventsPath = tempDir.resolve("failed-apply-events.jsonl");
        // Persist an event but simulate a repository failure
        try (JsonlEventStore firstStore = openStore(eventsPath)) {
            UserRepository failingRepository = mock(UserRepository.class);
            when(failingRepository.findByIdentification(anyString())).thenReturn(Optional.empty());
            when(failingRepository.findByEmail(anyString())).thenReturn(Optional.empty());
            doThrow(new IllegalStateException("Repository failed")).when(failingRepository).save(any(User.class));
            SnapshotCoordinator snapshots = mock(SnapshotCoordinator.class);
            PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(firstStore, snapshots);
            UserCommandService commands = new UserCommandService(failingRepository, new EventWriter(firstStore), operations);
            // Act & Assert
            IllegalStateException failure = assertThrows(IllegalStateException.class, () -> commands.register("1090123456", "Carlos", "Perez", "carlos@gmail.com"));
            assertEquals("Repository failed", failure.getMessage());
            assertEquals(1, firstStore.getLastSequence());
            IllegalStateException blocked = assertThrows(IllegalStateException.class, () -> commands.register("1090654321", "Maria", "Gomez", "maria@gmail.com"));
            assertEquals("Persistence requires recovery", blocked.getMessage());
            assertThrows(IllegalStateException.class, operations::saveSnapshot);
            assertEquals(1, firstStore.getLastSequence());
            verify(failingRepository, times(1)).save(any(User.class));
            verifyNoInteractions(snapshots);
        }
        // Simulate restart and recover the successfully persisted event
        try (JsonlEventStore secondStore = openStore(eventsPath)) {
            InMemoryUserRepository restoredRepository = new InMemoryUserRepository();
            SnapshotStore snapshotStore = mock(SnapshotStore.class);
            when(snapshotStore.loadLatest()).thenReturn(Optional.empty());
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(new UserSnapshotService(restoredRepository)));
            EventReplayCoordinator replay = new EventReplayCoordinator(secondStore, List.of(new UserEventHandler(restoredRepository)));
            SystemRecoveryCoordinator recovery = new SystemRecoveryCoordinator(snapshots, replay);
            // Act
            long lastSequence = recovery.recover();
            // Assert
            assertEquals(1, lastSequence);
            assertEquals(1, restoredRepository.findAll().size());
            User restored = restoredRepository.findByIdentification("1090123456").orElseThrow();
            assertEquals("Carlos", restored.getFirstName());
            assertEquals("Perez", restored.getLastName());
            assertEquals("carlos@gmail.com", restored.getEmail());
            assertTrue(restoredRepository.findByIdentification("1090654321").isEmpty());
        }
    }

}

