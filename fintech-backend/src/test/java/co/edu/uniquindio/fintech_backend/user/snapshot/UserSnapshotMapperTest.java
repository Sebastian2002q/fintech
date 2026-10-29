package co.edu.uniquindio.fintech_backend.user.snapshot;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserSnapshotMapperTest {

    @Test
    void shouldConvertUserToSnapshotEntry() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        user.changeRole(Role.ADMIN);
        user.block();
        // Act
        UserSnapshotEntry entry = UserSnapshotMapper.toEntry(user);
        // Assert
        assertNotNull(entry);
        assertEquals(user.getId(), entry.id());
        assertEquals(user.getIdentification(), entry.identification());
        assertEquals(user.getFirstName(), entry.firstName());
        assertEquals(user.getLastName(), entry.lastName());
        assertEquals(user.getEmail(), entry.email());
        assertEquals(user.getRole(), entry.role());
        assertEquals(user.getStatus(), entry.status());
        assertEquals(user.getCreatedAt(), entry.createdAt());
        assertEquals(Role.ADMIN, entry.role());
        assertEquals(UserStatus.BLOCKED, entry.status());
    }

    @Test
    void shouldRestoreUserFromSnapshotEntry() {
        // Arrange
        UUID originalId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.parse("2026-01-15T10:30:00Z");
        UserSnapshotEntry entry = new UserSnapshotEntry(originalId, "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.ADMIN, UserStatus.BLOCKED, originalCreatedAt);
        // Act
        User restoredUser = UserSnapshotMapper.toUser(entry);
        // Assert
        assertNotNull(restoredUser);
        assertEquals(originalId, restoredUser.getId());
        assertEquals("1090123456", restoredUser.getIdentification());
        assertEquals("Carlos", restoredUser.getFirstName());
        assertEquals("Perez", restoredUser.getLastName());
        assertEquals("carlos@gmail.com", restoredUser.getEmail());
        assertEquals(Role.ADMIN, restoredUser.getRole());
        assertEquals(UserStatus.BLOCKED, restoredUser.getStatus());
        assertEquals(originalCreatedAt, restoredUser.getCreatedAt());
        assertFalse(restoredUser.isActive());
    }

    @Test
    void shouldRejectNullArguments() {
        // Act & Assert - Null user
        assertThrows(NullPointerException.class, () -> UserSnapshotMapper.toEntry(null));
        // Act & Assert - Null snapshot entry
        assertThrows(NullPointerException.class, () -> UserSnapshotMapper.toUser(null));
    }

}
