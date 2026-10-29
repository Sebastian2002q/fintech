package co.edu.uniquindio.fintech_backend.user.model;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldRestoreUserPreservingOriginalData() {
        // Arrange
        UUID originalId = UUID.randomUUID();
        Instant originalCreatedAt = Instant.parse("2026-10-20T10:30:00Z");
        // Act
        User restoredUser = User.restore(originalId, "1234567890", "Carlos", "Quintero", "carlos@gmail.com", Role.ADMIN, UserStatus.BLOCKED, originalCreatedAt);
        // Assert
        assertEquals(originalId, restoredUser.getId());
        assertEquals("1234567890", restoredUser.getIdentification());
        assertEquals("Carlos", restoredUser.getFirstName());
        assertEquals("Quintero", restoredUser.getLastName());
        assertEquals("carlos@gmail.com", restoredUser.getEmail());
        assertEquals(Role.ADMIN, restoredUser.getRole());
        assertEquals(UserStatus.BLOCKED, restoredUser.getStatus());
        assertEquals(originalCreatedAt, restoredUser.getCreatedAt());
    }

    @Test
    void shouldRejectInvalidDataDuringRestoration() {
        // Arrange
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.now();
        // Act & Assert
        assertThrows(NullPointerException.class, () -> User.restore(null, "1234567890", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, createdAt));
        assertThrows(IllegalArgumentException.class, () -> User.restore(id, "", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, createdAt));
        assertThrows(NullPointerException.class, () -> User.restore(id, "1234567890", "Carlos", "Perez", "carlos@gmail.com", null, UserStatus.ACTIVE, createdAt));
        assertThrows(NullPointerException.class, () -> User.restore(id, "1234567890", "Carlos", "Perez", "carlos@gmail.com", Role.USER, null, createdAt));
        assertThrows(NullPointerException.class, () -> User.restore(id, "1234567890", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, null));
    }

    @Test
    void shouldCreateUserWithDefaultValues() {
        // Arrange
        Instant beforeCreation = Instant.now();
        // Act
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        Instant afterCreation = Instant.now();
        // Assert
        assertNotNull(user.getId());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertNotNull(user.getCreatedAt());
        assertFalse(user.getCreatedAt().isBefore(beforeCreation));
        assertFalse(user.getCreatedAt().isAfter(afterCreation));
        assertTrue(user.isActive());
    }

    @Test
    void shouldRejectBlankDataWhenCreatingUser() {
        // Act & Assert
        // Blank identification
        assertThrows(IllegalArgumentException.class, () -> new User("", "Carlos", "Perez", "carlos@gmail.com"));
        // Blank first name
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", "   ", "Perez", "carlos@gmail.com"));
        // Blank last name
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", "Carlos", "", "carlos@gmail.com"));
        // Blank email
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", "Carlos", "Perez", "   "));
    }

    @Test
    void shouldUpdateProfileWithoutChangingIdentityAndStatus() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        String originalIdentification = user.getIdentification();
        Instant originalCreatedAt = user.getCreatedAt();
        Role originalRole = user.getRole();
        UserStatus originalStatus = user.getStatus();
        // Act
        user.updateProfile("Carlos Alberto", "Quintero", "carlos@hotmail.com");
        // Assert
        assertEquals("Carlos Alberto", user.getFirstName());
        assertEquals("Quintero", user.getLastName());
        assertEquals("carlos@hotmail.com", user.getEmail());
        assertEquals(originalId, user.getId());
        assertEquals(originalIdentification, user.getIdentification());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals(originalRole, user.getRole());
        assertEquals(originalStatus, user.getStatus());
    }

    @Test
    void shouldRejectInvalidProfileUpdateWithoutChangingData() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile("Carlos Alberto", "Quintero", "   "));
        // Verify that the original data remains unchanged
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
    }

    @Test
    void shouldChangeUserRole() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();
        assertEquals(Role.USER, user.getRole());
        // Act
        user.changeRole(Role.ADMIN);
        // Assert
        assertEquals(Role.ADMIN, user.getRole());
        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(UserStatus.ACTIVE, user.getStatus());
    }

    @Test
    void shouldRejectNullRoleWithoutChangingUser() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        assertEquals(Role.USER, user.getRole());
        // Act & Assert
        assertThrows(NullPointerException.class, () -> user.changeRole(null));
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldBlockUserWithoutChangingOtherData() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
        // Act
        user.block();
        // Assert
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertFalse(user.isActive());
        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldDeactivateUserWithoutChangingOtherData() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
        // Act
        user.deactivate();
        // Assert
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertFalse(user.isActive());
        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldActivateInactiveUser() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();
        user.deactivate();
        assertEquals(UserStatus.INACTIVE, user.getStatus());
        assertFalse(user.isActive());
        // Act
        user.activate();
        // Assert
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldActivateBlockedUser() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        UUID originalId = user.getId();
        Instant originalCreatedAt = user.getCreatedAt();
        user.block();
        assertEquals(UserStatus.BLOCKED, user.getStatus());
        assertFalse(user.isActive());
        // Act
        user.activate();
        // Assert
        assertEquals(UserStatus.ACTIVE, user.getStatus());
        assertTrue(user.isActive());
        assertEquals(originalId, user.getId());
        assertEquals(originalCreatedAt, user.getCreatedAt());
        assertEquals("1090123456", user.getIdentification());
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        assertEquals(Role.USER, user.getRole());
    }

    @Test
    void shouldCreateIndependentUserCopy() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        original.changeRole(Role.ADMIN);
        original.block();
        // Act
        User copy = new User(original);
        // Assert - Verify copied data
        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getIdentification(), copy.getIdentification());
        assertEquals(original.getFirstName(), copy.getFirstName());
        assertEquals(original.getLastName(), copy.getLastName());
        assertEquals(original.getEmail(), copy.getEmail());
        assertEquals(original.getRole(), copy.getRole());
        assertEquals(original.getStatus(), copy.getStatus());
        assertEquals(original.getCreatedAt(), copy.getCreatedAt());
        // Modify the original user
        original.updateProfile("Carlos Alberto", "Quintero", "carlos@hotmail.com");
        original.changeRole(Role.USER);
        original.activate();
        // Verify that the copy remains unchanged
        assertEquals("Carlos", copy.getFirstName());
        assertEquals("Perez", copy.getLastName());
        assertEquals("carlos@gmail.com", copy.getEmail());
        assertEquals(Role.ADMIN, copy.getRole());
        assertEquals(UserStatus.BLOCKED, copy.getStatus());
        // Modify the copy
        copy.deactivate();
        // Verify that the original remains unchanged
        assertEquals(UserStatus.ACTIVE, original.getStatus());
        assertEquals(UserStatus.INACTIVE, copy.getStatus());
    }

    @Test
    void shouldRejectNullUserWhenCopying() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new User((User) null));
    }

    @Test
    void shouldNormalizeEmailWhenCreatingUser() {
        // Arrange
        String email = "  Carlos@GMAIL.COM  ";
        // Act
        User user = new User("1090123456", "Carlos", "Perez", email);
        // Assert
        assertEquals("carlos@gmail.com", user.getEmail());
    }

    @Test
    void shouldNormalizeEmailWhenUpdatingProfile() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act
        user.updateProfile("Carlos Alberto", "Quintero", "  CARLOS@HOTMAIL.COM  ");
        // Assert
        assertEquals("Carlos Alberto", user.getFirstName());
        assertEquals("Quintero", user.getLastName());
        assertEquals("carlos@hotmail.com", user.getEmail());
    }


    @Test
    void shouldRejectNullDataWhenCreatingUser() {
        // Null identification
        assertThrows(IllegalArgumentException.class, () -> new User(null, "Carlos", "Perez", "carlos@gmail.com"));
        // Null first name
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", null, "Perez", "carlos@gmail.com"));
        // Null last name
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", "Carlos", null, "carlos@gmail.com"));
        // Null email
        assertThrows(IllegalArgumentException.class, () -> new User("1090123456", "Carlos", "Perez", null));
    }

    @Test
    void shouldRejectNullProfileDataWithoutChangingUser() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert - Null first name
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile(null, "Quintero", "nuevo@gmail.com"));
        // Null last name
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile("Carlos Alberto", null, "nuevo@gmail.com"));
        // Null email
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile("Carlos Alberto", "Quintero", null));
        // Verify that the original data remains unchanged
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
    }

    @Test
    void shouldRejectBlankNamesWithoutChangingProfile() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act & Assert - Blank first name
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile("   ", "Quintero", "nuevo@gmail.com"));
        // Verify that no changes were applied
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
        // Act & Assert - Blank last name
        assertThrows(IllegalArgumentException.class, () -> user.updateProfile("Carlos Alberto", "   ", "nuevo@gmail.com"));
        // Verify that no changes were applied
        assertEquals("Carlos", user.getFirstName());
        assertEquals("Perez", user.getLastName());
        assertEquals("carlos@gmail.com", user.getEmail());
    }

}
