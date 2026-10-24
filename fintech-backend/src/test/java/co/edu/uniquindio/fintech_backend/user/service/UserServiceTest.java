package co.edu.uniquindio.fintech_backend.user.service;

import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.NoSuchElementException;

import static org.junit.jupiter.api.Assertions.*;

class UserServiceTest {
    private UserRepository userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        userService = new UserService(userRepository);
    }

    @Test
    void shouldGetUserById() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        userRepository.save(user);
        // Act
        User result = userService.getById(user.getId());
        // Assert
        assertEquals(user.getId(), result.getId());
        assertEquals(user.getEmail(), result.getEmail());
    }

    @Test
    void shouldThrowWhenUserDoesNotExist() {
        // Arrange
        UUID unknownId = UUID.randomUUID();
        // Act & Assert
        assertThrows(NoSuchElementException.class, () -> userService.getById(unknownId));
    }

    @Test
    void shouldGetAllUsers() {
        // Arrange
        User user1 = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        User user2 = new User("1090654321", "Laura", "Gomez", "laura@gmail.com");
        userRepository.save(user1);
        userRepository.save(user2);
        // Act
        var users = userService.getAll();
        // Assert
        assertEquals(2, users.size());
    }
}
