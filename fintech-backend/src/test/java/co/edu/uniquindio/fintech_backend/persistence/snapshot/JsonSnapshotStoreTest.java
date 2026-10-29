package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JsonSnapshotStoreTest {
    @TempDir
    Path tempDir;

    private JsonSnapshotStore snapshotStore;
    private JsonMapper jsonMapper;
    private Path filePath;

    @BeforeEach
    void setUp() {
        filePath = tempDir.resolve("snapshot.json");
        jsonMapper = JsonMapper.builder().build();
        snapshotStore = new JsonSnapshotStore(filePath.toString(), jsonMapper);
    }

    @Test
    void shouldSaveAndLoadSnapshot() {
        // Arrange
        JsonNode userState = jsonMapper.readTree("""
                {
                    "users": [
                        {
                            "id": "user-001",
                            "firstName": "Carlos",
                            "email": "carlos@gmail.com"
                        }
                    ]
                }
                """);
        StoredSnapshot snapshot = new StoredSnapshot(1, 10L, Instant.now(), Map.of("user", userState));
        // Act
        snapshotStore.save(snapshot);
        Optional<StoredSnapshot> result = snapshotStore.loadLatest();
        // Assert
        assertTrue(Files.exists(filePath));
        assertTrue(result.isPresent());
        StoredSnapshot recoveredSnapshot = result.orElseThrow();
        assertEquals(snapshot, recoveredSnapshot);
        assertEquals(1, recoveredSnapshot.schemaVersion());
        assertEquals(10L, recoveredSnapshot.lastSequence());
        assertEquals(userState, recoveredSnapshot.moduleStates().get("user"));
    }

    @Test
    void shouldReturnEmptyWhenSnapshotDoesNotExist() {
        // Arrange
        assertFalse(Files.exists(filePath));
        // Act
        Optional<StoredSnapshot> result = snapshotStore.loadLatest();
        // Assert
        assertTrue(result.isEmpty());
        assertFalse(Files.exists(filePath));
    }

    @Test
    void shouldRejectOlderSnapshot() {
        // Arrange
        StoredSnapshot currentSnapshot = new StoredSnapshot(1, 100L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[{\"firstName\":\"Carlos\"}]}")));
        StoredSnapshot olderSnapshot = new StoredSnapshot(1, 50L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[]}")));
        snapshotStore.save(currentSnapshot);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> snapshotStore.save(olderSnapshot));
        StoredSnapshot recoveredSnapshot = snapshotStore.loadLatest().orElseThrow();
        assertEquals(currentSnapshot, recoveredSnapshot);
        assertEquals(100L, recoveredSnapshot.lastSequence());
    }

    @Test
    void shouldRejectSnapshotWithSameSequence() {
        // Arrange
        StoredSnapshot currentSnapshot = new StoredSnapshot(1, 100L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[{\"firstName\":\"Carlos\"}]}")));
        StoredSnapshot duplicateSnapshot = new StoredSnapshot(1, 100L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[{\"firstName\":\"Pedro\"}]}")));
        snapshotStore.save(currentSnapshot);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> snapshotStore.save(duplicateSnapshot));
        StoredSnapshot recoveredSnapshot = snapshotStore.loadLatest().orElseThrow();
        assertEquals(currentSnapshot, recoveredSnapshot);
        assertEquals(100L, recoveredSnapshot.lastSequence());
    }

    @Test
    void shouldRejectCorruptedSnapshot() throws Exception {
        // Arrange
        String corruptedJson = """
                {
                    "schemaVersion": 1,
                    "lastSequence": 100,
                    "moduleStates":
                """;
        Files.writeString(filePath, corruptedJson);
        // Act & Assert
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> snapshotStore.loadLatest());
        assertEquals("Invalid snapshot JSON", exception.getMessage());
        assertInstanceOf(tools.jackson.core.JacksonException.class, exception.getCause());
    }

    @Test
    void shouldReplaceSnapshotWithNewerSequence() {
        // Arrange
        StoredSnapshot initialSnapshot = new StoredSnapshot(1, 100L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[{\"firstName\":\"Carlos\"}]}")));
        StoredSnapshot updatedSnapshot = new StoredSnapshot(1, 150L, Instant.now(), Map.of("user", jsonMapper.readTree("{\"users\":[{\"firstName\":\"Carlos Alberto\"}]}")));
        snapshotStore.save(initialSnapshot);
        // Act
        snapshotStore.save(updatedSnapshot);
        // Assert
        StoredSnapshot recoveredSnapshot = snapshotStore.loadLatest().orElseThrow();
        assertEquals(updatedSnapshot, recoveredSnapshot);
        assertEquals(150L, recoveredSnapshot.lastSequence());
        assertEquals("Carlos Alberto", recoveredSnapshot.moduleStates().get("user").get("users").get(0).get("firstName").asText());
    }

    @Test
    void shouldProtectSnapshotFromExternalModifications() {
        // Arrange
        JsonNode userState = jsonMapper.readTree("""
                {
                    "users": [
                        {
                            "firstName": "Carlos"
                        }
                    ]
                }
                """);
        StoredSnapshot snapshot = new StoredSnapshot(1, 100L, Instant.now(), Map.of("user", userState));
        // Act: Modify the original JSON
        ((tools.jackson.databind.node.ObjectNode) userState.get("users").get(0)).put("firstName", "Pedro");
        // Assert: The snapshot must remain unchanged
        assertEquals("Carlos", snapshot.moduleStates().get("user").get("users").get(0).get("firstName").asText());
        // Act: Modify JSON obtained from the snapshot
        JsonNode retrievedState = snapshot.moduleStates().get("user");
        ((tools.jackson.databind.node.ObjectNode) retrievedState.get("users").get(0)).put("firstName", "Andres");
        // Assert: The internal state must remain unchanged
        assertEquals("Carlos", snapshot.moduleStates().get("user").get("users").get(0).get("firstName").asText());
    }

    @Test
    void shouldRecoverSnapshotAfterRestart() {
        // Arrange
        StoredSnapshot originalSnapshot = new StoredSnapshot(1, 250L, Instant.now(), Map.of("user", jsonMapper.readTree("""
                {
                    "users": [
                        {
                            "firstName": "Carlos",
                            "email": "carlos@gmail.com"
                        }
                    ]
                }
                """), "wallet", jsonMapper.readTree("""
                {
                    "wallets": [
                        {
                            "balance": 500000
                        }
                    ]
                }
                """)));
        snapshotStore.save(originalSnapshot);
        // Act: Simulate a new application instance
        JsonSnapshotStore recoveredStore = new JsonSnapshotStore(filePath.toString(), JsonMapper.builder().build());
        StoredSnapshot recoveredSnapshot = recoveredStore.loadLatest().orElseThrow();
        // Assert
        assertEquals(originalSnapshot, recoveredSnapshot);
        assertEquals(250L, recoveredSnapshot.lastSequence());
        assertEquals(2, recoveredSnapshot.moduleStates().size());
        assertTrue(recoveredSnapshot.moduleStates().containsKey("user"));
        assertTrue(recoveredSnapshot.moduleStates().containsKey("wallet"));
    }
}
