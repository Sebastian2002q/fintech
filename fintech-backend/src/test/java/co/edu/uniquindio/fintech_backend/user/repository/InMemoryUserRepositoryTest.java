package co.edu.uniquindio.fintech_backend.user.repository;

import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.Arrays;

import java.util.List;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryUserRepositoryTest {
    private InMemoryUserRepository repository;

    @BeforeEach
    void setUp() {
        repository = new InMemoryUserRepository();
    }

    @Test
    void shouldSaveAndFindUserById() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        // Act
        repository.save(user);
        UUID id = user.getId();
        User savedUser = repository.findById(id).orElseThrow();
        // Assert
        assertEquals(user.getId(), savedUser.getId());
        assertEquals(user.getEmail(), savedUser.getEmail());
        assertEquals(user.getIdentification(), savedUser.getIdentification());
    }

    @Test
    void shouldRejectDuplicateIdentification() {
        // Arrange
        User user1 = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User user2 = new User("1090123456", "Laura", "Gomez", "laura@gmail.com");
        repository.save(user1);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.save(user2));
        assertTrue(repository.findById(user1.getId()).isPresent());
        assertTrue(repository.findById(user2.getId()).isEmpty());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Arrange
        User user1 = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User user2 = new User("1090654321", "Laura", "Gomez", "carlos@gmail.com");
        repository.save(user1);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.save(user2));
        assertTrue(repository.findById(user1.getId()).isPresent());
        assertTrue(repository.findById(user2.getId()).isEmpty());
    }

    @Test
    void shouldUpdateEmailIndex() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        // Act
        user.updateProfile("Carlos", "Perez", "carlos@hotmail.com");
        repository.save(user);
        // Assert
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        assertTrue(repository.findByEmail("carlos@hotmail.com").isPresent());
        User updatedUser = repository.findById(user.getId()).orElseThrow();
        assertEquals("carlos@hotmail.com", updatedUser.getEmail());
        assertEquals(user.getId(), updatedUser.getId());
        assertEquals("1090123456", updatedUser.getIdentification());
    }

    @Test
    void shouldProtectStoredUserFromExternalChanges() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        // Act
        User retrievedUser = repository.findById(user.getId()).orElseThrow();
        retrievedUser.updateProfile("Carlos", "Perez", "carlos@hotmail.com");
        // Assert
        User storedUser = repository.findById(user.getId()).orElseThrow();
        assertEquals("carlos@gmail.com", storedUser.getEmail());
        assertEquals("carlos@hotmail.com", retrievedUser.getEmail());
        assertTrue(repository.findByEmail("carlos@gmail.com").isPresent());
        assertTrue(repository.findByEmail("carlos@hotmail.com").isEmpty());
    }

    @Test
    void shouldReturnEmptyForNonexistentIdentificationAndEmail() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        // Act & Assert
        assertTrue(repository.findByIdentification("9999999999").isEmpty());
        assertTrue(repository.findByEmail("unknown@gmail.com").isEmpty());
        // The existing user must remain unchanged
        assertTrue(repository.findById(user.getId()).isPresent());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldReplaceAllUsers() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User originalUser = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(originalUser);
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        // Act
        repository.replaceAll(List.of(firstUser, secondUser));
        // Assert - Old user was removed
        assertTrue(repository.findById(originalUser.getId()).isEmpty());
        assertTrue(repository.findByIdentification("1000000001").isEmpty());
        assertTrue(repository.findByEmail("original@gmail.com").isEmpty());
        // Assert - New users were stored
        assertEquals(2, repository.findAll().size());
        assertTrue(repository.findById(firstUser.getId()).isPresent());
        assertTrue(repository.findById(secondUser.getId()).isPresent());
        // Assert - Identification indexes were rebuilt
        assertEquals(firstUser.getId(), repository.findByIdentification("1090123456").orElseThrow().getId());
        assertEquals(secondUser.getId(), repository.findByIdentification("1090654321").orElseThrow().getId());
        // Assert - Email indexes were rebuilt
        assertEquals(firstUser.getId(), repository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        assertEquals(secondUser.getId(), repository.findByEmail("maria@gmail.com").orElseThrow().getId());
    }

    @Test
    void shouldRejectDuplicateIdsDuringReplaceAll() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = User.restore(firstUser.getId(), "1090654321", "Maria", "Gomez", "maria@gmail.com", firstUser.getRole(), firstUser.getStatus(), firstUser.getCreatedAt());
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.replaceAll(List.of(firstUser, secondUser)));
        // Verify that the original repository remains unchanged
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        assertTrue(repository.findById(firstUser.getId()).isEmpty());
    }

    @Test
    void shouldRejectDuplicateIdentificationsDuringReplaceAll() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090123456", "Maria", "Gomez", "maria@gmail.com");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.replaceAll(List.of(firstUser, secondUser)));
        // Verify that the original repository remains unchanged
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        assertTrue(repository.findByIdentification("1000000001").isPresent());
        assertTrue(repository.findByIdentification("1090123456").isEmpty());
    }

    @Test
    void shouldRejectDuplicateEmailsDuringReplaceAll() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090654321", "Maria", "Gomez", "  CARLOS@GMAIL.COM  ");
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> repository.replaceAll(List.of(firstUser, secondUser)));
        // Verify that the original repository remains unchanged
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findByEmail("original@gmail.com").isPresent());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
    }

    @Test
    void shouldRejectNullCollectionDuringReplaceAll() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> repository.replaceAll(null));
        // Verify that the original repository remains unchanged
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
    }

    @Test
    void shouldRejectNullUserDuringReplaceAll() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User original = new User("1000000001", "Original", "User", "original@gmail.com");
        repository.save(original);
        User validUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        List<User> users = Arrays.asList(validUser, null);
        // Act & Assert
        assertThrows(NullPointerException.class, () -> repository.replaceAll(users));
        // Verify that no partial replacement occurred
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(original.getId()).isPresent());
        assertTrue(repository.findById(validUser.getId()).isEmpty());
    }

    @Test
    void shouldProtectReplacedUsersFromExternalChanges() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.replaceAll(List.of(user));
        // Act - Modify the original object
        user.updateProfile("Carlos Alberto", "Quintero", "nuevo@gmail.com");
        user.block();
        // Assert - Stored user remains unchanged
        User storedUser = repository.findById(user.getId()).orElseThrow();
        assertEquals("Carlos", storedUser.getFirstName());
        assertEquals("Perez", storedUser.getLastName());
        assertEquals("carlos@gmail.com", storedUser.getEmail());
        assertEquals(UserStatus.ACTIVE, storedUser.getStatus());
        // Verify that the email index remains consistent
        assertTrue(repository.findByEmail("carlos@gmail.com").isPresent());
        assertTrue(repository.findByEmail("nuevo@gmail.com").isEmpty());
        // Act - Modify the retrieved copy
        storedUser.deactivate();
        // Assert - Repository remains unchanged
        assertEquals(UserStatus.ACTIVE, repository.findById(user.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldReplaceAllUsersWithEmptyCollection() {
        // Arrange
        InMemoryUserRepository repository = new InMemoryUserRepository();
        User firstUser = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User secondUser = new User("1090654321", "Maria", "Gomez", "maria@gmail.com");
        repository.save(firstUser);
        repository.save(secondUser);
        // Act
        repository.replaceAll(List.of());
        // Assert
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findById(firstUser.getId()).isEmpty());
        assertTrue(repository.findById(secondUser.getId()).isEmpty());
        assertTrue(repository.findByIdentification("1090123456").isEmpty());
        assertTrue(repository.findByIdentification("1090654321").isEmpty());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        assertTrue(repository.findByEmail("maria@gmail.com").isEmpty());
    }


}
