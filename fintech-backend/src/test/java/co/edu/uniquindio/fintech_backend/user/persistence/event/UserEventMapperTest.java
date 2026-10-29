package co.edu.uniquindio.fintech_backend.user.persistence.event;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserEventMapperTest {
    @Test
    void shouldConvertUserToEventData() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        user.changeRole(Role.ADMIN);
        user.block();
        // Act
        Map<String, String> data = UserEventMapper.toData(user);
        // Assert
        assertEquals(8, data.size());
        assertEquals(user.getId().toString(), data.get("id"));
        assertEquals("1090123456", data.get("identification"));
        assertEquals("Carlos", data.get("firstName"));
        assertEquals("Perez", data.get("lastName"));
        assertEquals("carlos@gmail.com", data.get("email"));
        assertEquals("ADMIN", data.get("role"));
        assertEquals("BLOCKED", data.get("status"));
        assertEquals(user.getCreatedAt().toString(), data.get("createdAt"));
    }

    @Test
    void shouldRestoreUserFromEventData() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-01-15T10:30:00Z");
        Map<String, String> data = Map.of("id", id.toString(), "identification", "1090123456", "firstName", "Carlos", "lastName", "Perez", "email", "carlos@gmail.com", "role", "ADMIN", "status", "BLOCKED", "createdAt", createdAt.toString());
        // Act
        User user = UserEventMapper.toUser(data);
        // Assert
        assertEquals(id, user.getId());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertEquals(createdAt, user.getCreatedAt());
        assertFalse(user.isActive());
    }

    @Test
    void shouldPreserveUserDataDuringRoundTrip() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        original.changeRole(Role.SUPERADMIN);
        original.deactivate();
        // Act
        User restored = UserEventMapper.toUser(UserEventMapper.toData(original));
        // Assert
        assertNotSame(original, restored);
        assertEquals(original.getId(), restored.getId());
        assertEquals(original.getIdentification(), restored.getIdentification());
        assertEquals(original.getFirstName(), restored.getFirstName());
        assertEquals(original.getLastName(), restored.getLastName());
        assertEquals(original.getEmail(), restored.getEmail());
        assertEquals(original.getRole(), restored.getRole());
        assertEquals(original.getStatus(), restored.getStatus());
        assertEquals(original.getCreatedAt(), restored.getCreatedAt());
    }

    @Test
    void shouldRejectNullArguments() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> UserEventMapper.toData(null));
        assertThrows(NullPointerException.class, () -> UserEventMapper.toUser(null));
    }

    @Test
    void shouldRejectInvalidEventData() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        Map<String, String> missingField = new HashMap<>(UserEventMapper.toData(user));
        Map<String, String> invalidId = new HashMap<>(UserEventMapper.toData(user));
        Map<String, String> invalidRole = new HashMap<>(UserEventMapper.toData(user));
        Map<String, String> invalidStatus = new HashMap<>(UserEventMapper.toData(user));
        missingField.remove("email");
        invalidId.put("id", "invalid-uuid");
        invalidRole.put("role", "INVALID");
        invalidStatus.put("status", "INVALID");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> UserEventMapper.toUser(missingField));
        assertThrows(IllegalArgumentException.class, () -> UserEventMapper.toUser(invalidId));
        assertThrows(IllegalArgumentException.class, () -> UserEventMapper.toUser(invalidRole));
        assertThrows(IllegalArgumentException.class, () -> UserEventMapper.toUser(invalidStatus));
    }
}
