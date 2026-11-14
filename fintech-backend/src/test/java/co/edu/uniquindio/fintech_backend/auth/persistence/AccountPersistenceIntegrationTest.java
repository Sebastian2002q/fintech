package co.edu.uniquindio.fintech_backend.auth.persistence;

import co.edu.uniquindio.fintech_backend.auth.persistence.event.AccountRegistrationEventHandler;
import co.edu.uniquindio.fintech_backend.auth.persistence.event.CredentialEventHandler;
import co.edu.uniquindio.fintech_backend.auth.persistence.service.AuthCommandService;
import co.edu.uniquindio.fintech_backend.auth.persistence.snapshot.CredentialSnapshotService;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.auth.service.PasswordHasher;
import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventReplayCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.persistence.event.JsonlEventStore;
import co.edu.uniquindio.fintech_backend.persistence.recovery.SystemRecoveryCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.JsonSnapshotStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventHandler;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountPersistenceIntegrationTest {
    @TempDir
    Path tempDir;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final PasswordHasher hasher = new PasswordHasher();

    private JsonlEventStore openStore(Path path) {
        return new JsonlEventStore(path.toString(), mapper);
    }

    private SnapshotCoordinator snapshots(Path path, InMemoryUserRepository users, InMemoryCredentialRepository credentials) {
        return new SnapshotCoordinator(new JsonSnapshotStore(path.toString(), mapper), mapper, List.of(new UserSnapshotService(users), new CredentialSnapshotService(credentials)));
    }

    private AuthCommandService commands(JsonlEventStore store, SnapshotCoordinator snapshots, InMemoryUserRepository users, InMemoryCredentialRepository credentials) {
        return new AuthCommandService(users, credentials, hasher, new EventWriter(store), new PersistenceOperationCoordinator(store, snapshots));
    }

    private SystemRecoveryCoordinator recovery(JsonlEventStore store, SnapshotCoordinator snapshots, InMemoryUserRepository users, InMemoryCredentialRepository credentials) {
        EventReplayCoordinator replay = new EventReplayCoordinator(store, List.of(new UserEventHandler(users), new CredentialEventHandler(users, credentials), new AccountRegistrationEventHandler(users, credentials)));
        return new SystemRecoveryCoordinator(snapshots, replay);
    }

    @Test
    void shouldRecoverRegisteredAccountAfterRestart() throws Exception {
        // Arrange
        Path eventsPath = tempDir.resolve("events.jsonl");
        Path snapshotPath = tempDir.resolve("snapshot.json");
        String password = "MySecurePassword2026!";
        User original;
        String originalHash;
        // Register account and persist its event
        try (JsonlEventStore store = openStore(eventsPath)) {
            InMemoryUserRepository users = new InMemoryUserRepository();
            InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
            AuthCommandService service = commands(store, snapshots(snapshotPath, users, credentials), users, credentials);
            original = service.register("1090123456", "Carlos", "Perez", " CARLOS@GMAIL.COM ", password);
            originalHash = credentials.findByUserId(original.getId()).orElseThrow().passwordHash();
            assertEquals(1, store.getLastSequence());
            assertEquals(1, store.readAfter(0).size());
            assertEquals(EventType.ACCOUNT_REGISTERED, store.readAfter(0).getFirst().type());
            assertTrue(hasher.matches(password, originalHash));
            assertEquals(1, users.findAll().size());
            assertEquals(1, credentials.findAll().size());
        }
        // Verify that the original password was not written to disk
        String json = Files.readString(eventsPath);
        assertTrue(json.contains("passwordHash"));
        assertFalse(json.contains(password));
        // Simulate restart with empty repositories
        try (JsonlEventStore store = openStore(eventsPath)) {
            InMemoryUserRepository restoredUsers = new InMemoryUserRepository();
            InMemoryCredentialRepository restoredCredentials = new InMemoryCredentialRepository();
            SystemRecoveryCoordinator coordinator = recovery(store, snapshots(snapshotPath, restoredUsers, restoredCredentials), restoredUsers, restoredCredentials);
            // Act
            long sequence = coordinator.recover();
            // Assert
            assertEquals(1, sequence);
            assertEquals(1, restoredUsers.findAll().size());
            assertEquals(1, restoredCredentials.findAll().size());
            User restored = restoredUsers.findById(original.getId()).orElseThrow();
            String restoredHash = restoredCredentials.findByUserId(original.getId()).orElseThrow().passwordHash();
            assertEquals(original.getId(), restored.getId());
            assertEquals(original.getIdentification(), restored.getIdentification());
            assertEquals(original.getFirstName(), restored.getFirstName());
            assertEquals(original.getLastName(), restored.getLastName());
            assertEquals(original.getEmail(), restored.getEmail());
            assertEquals(original.getRole(), restored.getRole());
            assertEquals(original.getStatus(), restored.getStatus());
            assertEquals(original.getCreatedAt(), restored.getCreatedAt());
            assertEquals("carlos@gmail.com", restored.getEmail());
            assertEquals(originalHash, restoredHash);
            assertTrue(hasher.matches(password, restoredHash));
            assertEquals(original.getId(), restoredUsers.findByEmail("carlos@gmail.com").orElseThrow().getId());
        }
    }

    @Test
    void shouldRecoverAccountsFromSnapshotAndSubsequentEvents() {
        // Arrange
        Path eventsPath = tempDir.resolve("events.jsonl");
        Path snapshotPath = tempDir.resolve("snapshot.json");
        UUID firstId;
        UUID secondId;
        String firstPassword = "FirstSecurePassword2026!";
        String secondPassword = "SecondSecurePassword2026!";
        // Register first account, save snapshot, then register second account
        try (JsonlEventStore store = openStore(eventsPath)) {
            InMemoryUserRepository users = new InMemoryUserRepository();
            InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
            SnapshotCoordinator snapshots = snapshots(snapshotPath, users, credentials);
            PersistenceOperationCoordinator operations = new PersistenceOperationCoordinator(store, snapshots);
            AuthCommandService service = new AuthCommandService(users, credentials, hasher, new EventWriter(store), operations);
            User first = service.register("1090123456", "Carlos", "Perez", "carlos@gmail.com", firstPassword);
            firstId = first.getId();
            operations.saveSnapshot();
            User second = service.register("1090654321", "Maria", "Gomez", "maria@gmail.com", secondPassword);
            secondId = second.getId();
            assertEquals(2, store.getLastSequence());
            assertEquals(2, users.findAll().size());
            assertEquals(2, credentials.findAll().size());
            assertEquals(1, new JsonSnapshotStore(snapshotPath.toString(), mapper).loadLatest().orElseThrow().lastSequence());
        }
        // Simulate restart
        try (JsonlEventStore store = openStore(eventsPath)) {
            InMemoryUserRepository restoredUsers = new InMemoryUserRepository();
            InMemoryCredentialRepository restoredCredentials = new InMemoryCredentialRepository();
            SystemRecoveryCoordinator coordinator = recovery(store, snapshots(snapshotPath, restoredUsers, restoredCredentials), restoredUsers, restoredCredentials);
            // Act
            long sequence = coordinator.recover();
            // Assert
            assertEquals(2, sequence);
            assertEquals(2, restoredUsers.findAll().size());
            assertEquals(2, restoredCredentials.findAll().size());
            assertEquals("Carlos", restoredUsers.findById(firstId).orElseThrow().getFirstName());
            assertEquals("Maria", restoredUsers.findById(secondId).orElseThrow().getFirstName());
            assertEquals(firstId, restoredUsers.findByEmail("carlos@gmail.com").orElseThrow().getId());
            assertEquals(secondId, restoredUsers.findByEmail("maria@gmail.com").orElseThrow().getId());
            String firstHash = restoredCredentials.findByUserId(firstId).orElseThrow().passwordHash();
            String secondHash = restoredCredentials.findByUserId(secondId).orElseThrow().passwordHash();
            assertTrue(hasher.matches(firstPassword, firstHash));
            assertTrue(hasher.matches(secondPassword, secondHash));
            assertFalse(hasher.matches(firstPassword, secondHash));
            assertFalse(hasher.matches(secondPassword, firstHash));
        }
    }
}
