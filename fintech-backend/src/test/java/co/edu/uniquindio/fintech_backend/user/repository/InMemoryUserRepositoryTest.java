package co.edu.uniquindio.fintech_backend.user.repository;

import co.edu.uniquindio.fintech_backend.user.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

}
