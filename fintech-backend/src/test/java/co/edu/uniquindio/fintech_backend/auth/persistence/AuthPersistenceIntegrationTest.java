package co.edu.uniquindio.fintech_backend.auth.persistence;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.persistence.event.CredentialEventHandler;
import co.edu.uniquindio.fintech_backend.auth.persistence.event.CredentialEventMapper;
import co.edu.uniquindio.fintech_backend.auth.persistence.snapshot.CredentialSnapshotService;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.auth.service.PasswordHasher;
import co.edu.uniquindio.fintech_backend.persistence.event.EventReplayCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.persistence.event.JsonlEventStore;
import co.edu.uniquindio.fintech_backend.persistence.recovery.SystemRecoveryCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.JsonSnapshotStore;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotStore;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventHandler;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AuthPersistenceIntegrationTest {
    @TempDir
    Path tempDir;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void shouldRecoverCredentialsFromSnapshotAndSubsequentEvents() {
        Path eventsPath = tempDir.resolve("events.jsonl");
        Path snapshotPath = tempDir.resolve("snapshot.json");
        UUID userId;
        try (JsonlEventStore store = new JsonlEventStore(eventsPath.toString(), mapper)) {
            InMemoryUserRepository users = new InMemoryUserRepository();
            InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
            EventWriter writer = new EventWriter(store);
            User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
            userId = user.getId();
            writer.record(EventType.USER_REGISTERED, UserEventMapper.toData(user));
            users.save(user);
            Credential original = new Credential(userId, hasher.hash("OriginalPassword2026!"));
            writer.record(EventType.CREDENTIAL_CREATED, CredentialEventMapper.toData(original));
            credentials.save(original);
            SnapshotStore snapshotStore = new JsonSnapshotStore(snapshotPath.toString(), mapper);
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(new UserSnapshotService(users), new CredentialSnapshotService(credentials)));
            snapshots.save(store.getLastSequence());
            Credential updated = new Credential(userId, hasher.hash("UpdatedPassword2026!"));
            writer.record(EventType.PASSWORD_CHANGED, CredentialEventMapper.toData(updated));
            credentials.save(updated);
            assertEquals(3, store.getLastSequence());
        }
        try (JsonlEventStore store = new JsonlEventStore(eventsPath.toString(), mapper)) {
            InMemoryUserRepository restoredUsers = new InMemoryUserRepository();
            InMemoryCredentialRepository restoredCredentials = new InMemoryCredentialRepository();
            SnapshotStore snapshotStore = new JsonSnapshotStore(snapshotPath.toString(), mapper);
            SnapshotCoordinator snapshots = new SnapshotCoordinator(snapshotStore, mapper, List.of(new UserSnapshotService(restoredUsers), new CredentialSnapshotService(restoredCredentials)));
            EventReplayCoordinator replay = new EventReplayCoordinator(store, List.of(new UserEventHandler(restoredUsers), new CredentialEventHandler(restoredUsers, restoredCredentials)));
            SystemRecoveryCoordinator recovery = new SystemRecoveryCoordinator(snapshots, replay);
            long lastSequence = recovery.recover();
            assertEquals(3, lastSequence);
            assertEquals(1, restoredUsers.findAll().size());
            assertEquals(1, restoredCredentials.findAll().size());
            User user = restoredUsers.findById(userId).orElseThrow();
            Credential credential = restoredCredentials.findByUserId(userId).orElseThrow();
            assertEquals("Carlos", user.getFirstName());
            assertEquals("carlos@gmail.com", user.getEmail());
            assertEquals(userId, credential.userId());
            assertTrue(hasher.matches("UpdatedPassword2026!", credential.passwordHash()));
            assertFalse(hasher.matches("OriginalPassword2026!", credential.passwordHash()));
            assertEquals(2, snapshotStore.loadLatest().orElseThrow().lastSequence());
        }
    }
}
