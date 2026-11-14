package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.UserEventMapper;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class AccountRegistrationEventHandlerTest {
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final AccountRegistrationEventHandler handler = new AccountRegistrationEventHandler(users, credentials);
    private final User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");

    private StoredEvent accountEvent(User account) {
        Map<String, String> data = new HashMap<>(UserEventMapper.toData(account));
        data.put("passwordHash", "{bcrypt}encoded-hash");
        return new StoredEvent(1, UUID.randomUUID(), EventType.ACCOUNT_REGISTERED, Instant.now(), data);
    }

    @Test
    void shouldSupportAccountRegisteredEvent() {
        assertEquals(Set.of(EventType.ACCOUNT_REGISTERED), handler.supportedTypes());
    }

    @Test
    void shouldRestoreUserAndCredential() {
        // Act
        handler.apply(accountEvent(user));
        // Assert
        User restored = users.findById(user.getId()).orElseThrow();
        Credential credential = credentials.findByUserId(user.getId()).orElseThrow();
        assertEquals(user.getId(), restored.getId());
        assertEquals(user.getIdentification(), restored.getIdentification());
        assertEquals(user.getEmail(), restored.getEmail());
        assertEquals(user.getCreatedAt(), restored.getCreatedAt());
        assertEquals("{bcrypt}encoded-hash", credential.passwordHash());
    }

    @Test
    void shouldRejectDuplicateUserId() {
        // Arrange
        users.save(user);
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(accountEvent(user)));
        assertTrue(credentials.findAll().isEmpty());
    }

    @Test
    void shouldRejectDuplicateIdentification() {
        // Arrange
        users.save(new User("1090123456", "Other", "User", "other@gmail.com"));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(accountEvent(user)));
        assertTrue(credentials.findAll().isEmpty());
        assertEquals(1, users.findAll().size());
    }

    @Test
    void shouldRejectDuplicateEmail() {
        // Arrange
        users.save(new User("1090999999", "Other", "User", "carlos@gmail.com"));
        // Act & Assert
        assertThrows(IllegalStateException.class, () -> handler.apply(accountEvent(user)));
        assertTrue(credentials.findAll().isEmpty());
        assertEquals(1, users.findAll().size());
    }

    @Test
    void shouldRejectMissingPasswordHash() {
        // Arrange
        StoredEvent event = new StoredEvent(1, UUID.randomUUID(), EventType.ACCOUNT_REGISTERED, Instant.now(), UserEventMapper.toData(user));
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event));
        assertTrue(users.findAll().isEmpty());
        assertTrue(credentials.findAll().isEmpty());
    }

    @Test
    void shouldRejectUnsupportedEventsAndNullDependencies() {
        // Arrange
        StoredEvent event = new StoredEvent(1, UUID.randomUUID(), EventType.USER_REGISTERED, Instant.now(), Map.of());
        // Act & Assert
        assertThrows(NullPointerException.class, () -> handler.apply(null));
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event));
        assertThrows(NullPointerException.class, () -> new AccountRegistrationEventHandler(null, credentials));
        assertThrows(NullPointerException.class, () -> new AccountRegistrationEventHandler(users, null));
    }
}
