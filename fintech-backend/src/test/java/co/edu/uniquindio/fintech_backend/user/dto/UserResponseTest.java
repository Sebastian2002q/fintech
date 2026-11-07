package co.edu.uniquindio.fintech_backend.user.dto;

import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserResponseTest {
    @Test
    void shouldMapUserToResponse() {
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UserResponse response = UserResponse.from(user);
        assertEquals(user.getId(), response.id());
        assertEquals(user.getIdentification(), response.identification());
        assertEquals(user.getFirstName(), response.firstName());
        assertEquals(user.getLastName(), response.lastName());
        assertEquals(user.getEmail(), response.email());
        assertEquals(Role.USER, response.role());
        assertEquals(UserStatus.ACTIVE, response.status());
        assertEquals(user.getCreatedAt(), response.createdAt());
    }

    @Test
    void shouldMapUpdatedUserData() {
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        user.updateProfile("Carlos Alberto", "Quintero", "carlos@hotmail.com");
        user.changeRole(Role.ADMIN);
        user.block();
        UserResponse response = UserResponse.from(user);
        assertEquals("Carlos Alberto", response.firstName());
        assertEquals("Quintero", response.lastName());
        assertEquals("carlos@hotmail.com", response.email());
        assertEquals(Role.ADMIN, response.role());
        assertEquals(UserStatus.BLOCKED, response.status());
    }

    @Test
    void shouldPreserveRestoredUserIdentity() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-10-20T10:30:00Z");
        User user = User.restore(id, "1090123456", "Carlos", "Perez", "carlos@gmail.com", Role.ADMIN, UserStatus.INACTIVE, createdAt);
        UserResponse response = UserResponse.from(user);
        assertEquals(id, response.id());
        assertEquals(createdAt, response.createdAt());
        assertEquals(Role.ADMIN, response.role());
        assertEquals(UserStatus.INACTIVE, response.status());
    }

    @Test
    void shouldRejectNullUser() {
        assertThrows(NullPointerException.class, () -> UserResponse.from(null));
    }
}
