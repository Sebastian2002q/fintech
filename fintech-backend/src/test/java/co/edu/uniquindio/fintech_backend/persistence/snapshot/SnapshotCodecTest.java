package co.edu.uniquindio.fintech_backend.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotEntry;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotState;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class SnapshotCodecTest {
    private final JsonMapper mapper = JsonMapper.builder().build();
    private final SnapshotCodec codec = new SnapshotCodec(mapper);

    private record TestState(String name, int count) {
    }

    @Test
    void shouldEncodeAndDecodeUserSnapshotState() {
        // Arrange
        UserSnapshotEntry firstEntry = new UserSnapshotEntry(UUID.randomUUID(), "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.ADMIN, UserStatus.BLOCKED, Instant.parse("2026-01-15T10:30:00Z"));
        UserSnapshotEntry secondEntry = new UserSnapshotEntry(UUID.randomUUID(), "1090654321", "Maria", "Gomez", "maria@gmail.com", Role.USER, UserStatus.ACTIVE, Instant.parse("2026-02-20T14:00:00Z"));
        UserSnapshotState original = new UserSnapshotState(List.of(firstEntry, secondEntry));
        // Act
        JsonNode node = codec.encode(original);
        UserSnapshotState restored = codec.decode(node, UserSnapshotState.class);
        // Assert
        assertNotNull(node);
        assertTrue(node.isObject());
        assertEquals(2, node.get("users").size());
        assertEquals(original, restored);
    }

    @Test
    void shouldEncodeAndDecodeEmptySnapshotState() {
        // Arrange
        UserSnapshotState original = new UserSnapshotState(List.of());
        // Act
        JsonNode node = codec.encode(original);
        UserSnapshotState restored = codec.decode(node, UserSnapshotState.class);
        // Assert
        assertEquals(original, restored);
        assertTrue(restored.users().isEmpty());
    }

    @Test
    void shouldSupportDifferentStateTypes() {
        // Arrange
        TestState original = new TestState("Example", 10);
        // Act
        JsonNode node = codec.encode(original);
        TestState restored = codec.decode(node, TestState.class);
        // Assert
        assertEquals(original, restored);
        assertEquals("Example", restored.name());
        assertEquals(10, restored.count());
    }

    @Test
    void shouldRejectNullArguments() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new SnapshotCodec(null));
        assertThrows(NullPointerException.class, () -> codec.encode(null));
        assertThrows(NullPointerException.class, () -> codec.decode(null, UserSnapshotState.class));
        assertThrows(NullPointerException.class, () -> codec.decode(mapper.valueToTree(new UserSnapshotState(List.of())), null));
    }

    @Test
    void shouldRejectInvalidSnapshotJson() {
        // Arrange
        JsonNode invalidNode = mapper.readTree("{\"users\":\"invalid\"}");
        // Act & Assert
        assertThrows(JacksonException.class, () -> codec.decode(invalidNode, UserSnapshotState.class));
    }
}
