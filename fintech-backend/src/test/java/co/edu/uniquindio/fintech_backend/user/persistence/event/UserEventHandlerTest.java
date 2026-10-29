package co.edu.uniquindio.fintech_backend.user.persistence.event;

import co.edu.uniquindio.fintech_backend.persistence.event.EventHandler;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventHandler;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Arrays;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class UserEventHandlerTest {
    private final InMemoryUserRepository repository = new InMemoryUserRepository();
    private final EventHandler handler = new UserEventHandler(repository);

    private StoredEvent createEvent(long sequence, EventType type, User user) {
        return new StoredEvent(sequence, UUID.randomUUID(), type, Instant.now(), UserEventMapper.toData(user));
    }

    @Test
    void shouldDeclareSupportedEventTypes() {
        // Assert
        assertEquals(Set.of(EventType.USER_REGISTERED, EventType.USER_UPDATED), handler.supportedTypes());
    }

    @Test
    void shouldApplyUserRegisteredEvent() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        StoredEvent event = createEvent(1, EventType.USER_REGISTERED, user);
        // Act
        handler.apply(event);
        // Assert
        assertEquals(1, repository.findAll().size());
        assertEquals(user.getId(), repository.findById(user.getId()).orElseThrow().getId());
        assertEquals(user.getId(), repository.findByIdentification("1090123456").orElseThrow().getId());
        assertEquals(user.getId(), repository.findByEmail("carlos@gmail.com").orElseThrow().getId());
        assertEquals(user.getCreatedAt(), repository.findById(user.getId()).orElseThrow().getCreatedAt());
    }

    @Test
    void shouldApplyUserUpdatedEvent() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        user.updateProfile("Carlos Alberto", "Quintero", "nuevo@gmail.com");
        user.changeRole(Role.ADMIN);
        user.block();
        StoredEvent event = createEvent(2, EventType.USER_UPDATED, user);
        // Act
        handler.apply(event);
        // Assert
        User restored = repository.findById(user.getId()).orElseThrow();
        assertEquals("Carlos Alberto", restored.getFirstName());
        assertEquals("Quintero", restored.getLastName());
        assertEquals("nuevo@gmail.com", restored.getEmail());
        assertEquals(Role.ADMIN, restored.getRole());
        assertEquals(UserStatus.BLOCKED, restored.getStatus());
        assertEquals(user.getCreatedAt(), restored.getCreatedAt());
        assertEquals(user.getId(), repository.findByEmail("nuevo@gmail.com").orElseThrow().getId());
        assertTrue(repository.findByEmail("carlos@gmail.com").isEmpty());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRejectDuplicateUserRegistration() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(user);
        StoredEvent event = createEvent(2, EventType.USER_REGISTERED, user);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(event));
        assertEquals(1, repository.findAll().size());
        assertTrue(repository.findById(user.getId()).isPresent());
    }

    @Test
    void shouldRejectUpdateOfNonexistentUser() {
        // Arrange
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        StoredEvent event = createEvent(2, EventType.USER_UPDATED, user);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(event));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectChangesToImmutableUserData() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(original);
        User invalid = User.restore(original.getId(), "9999999999", "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, original.getCreatedAt());
        StoredEvent event = createEvent(2, EventType.USER_UPDATED, invalid);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(event));
        assertEquals("1090123456", repository.findById(original.getId()).orElseThrow().getIdentification());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRejectChangedCreationDate() {
        // Arrange
        User original = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        repository.save(original);
        User invalid = User.restore(original.getId(), original.getIdentification(), "Carlos", "Perez", "carlos@gmail.com", Role.USER, UserStatus.ACTIVE, original.getCreatedAt().plusSeconds(60));
        StoredEvent event = createEvent(2, EventType.USER_UPDATED, invalid);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(event));
        assertEquals(original.getCreatedAt(), repository.findById(original.getId()).orElseThrow().getCreatedAt());
    }

    @Test
    void shouldRejectUnsupportedAndNullEvents() {
        // Arrange
        EventType unsupported = Arrays.stream(EventType.values()).filter(type -> !handler.supportedTypes().contains(type)).findFirst().orElseThrow();
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        StoredEvent event = createEvent(1, unsupported, user);
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event));
        assertThrows(NullPointerException.class, () -> handler.apply(null));
        assertThrows(NullPointerException.class, () -> new UserEventHandler(null));
        assertTrue(repository.findAll().isEmpty());
    }
}
