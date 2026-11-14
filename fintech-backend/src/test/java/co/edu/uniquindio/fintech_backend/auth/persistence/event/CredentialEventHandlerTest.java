package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEventHandlerTest {
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final CredentialEventHandler handler = new CredentialEventHandler(users, credentials);
    private final User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");

    private StoredEvent event(EventType type, Map<String, String> data) {
        return new StoredEvent(1, UUID.randomUUID(), type, Instant.now(), data);
    }

    @Test
    void shouldSupportCredentialCreationAndPasswordChanges() {
        assertEquals(Set.of(EventType.CREDENTIAL_CREATED, EventType.PASSWORD_CHANGED), handler.supportedTypes());
    }

    @Test
    void shouldApplyCredentialCreatedEvent() {
        users.save(user);
        Credential credential = new Credential(user.getId(), "{bcrypt}first-hash");
        handler.apply(event(EventType.CREDENTIAL_CREATED, CredentialEventMapper.toData(credential)));
        assertEquals(credential, credentials.findByUserId(user.getId()).orElseThrow());
    }

    @Test
    void shouldRejectCredentialForUnknownUser() {
        Credential credential = new Credential(user.getId(), "{bcrypt}first-hash");
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.CREDENTIAL_CREATED, CredentialEventMapper.toData(credential))));
        assertTrue(credentials.findAll().isEmpty());
    }

    @Test
    void shouldRejectDuplicateCredential() {
        users.save(user);
        Credential original = new Credential(user.getId(), "{bcrypt}original-hash");
        credentials.save(original);
        Credential duplicate = new Credential(user.getId(), "{bcrypt}another-hash");
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.CREDENTIAL_CREATED, CredentialEventMapper.toData(duplicate))));
        assertEquals(original, credentials.findByUserId(user.getId()).orElseThrow());
    }

    @Test
    void shouldApplyPasswordChangedEvent() {
        users.save(user);
        credentials.save(new Credential(user.getId(), "{bcrypt}old-hash"));
        Credential updated = new Credential(user.getId(), "{bcrypt}new-hash");
        handler.apply(event(EventType.PASSWORD_CHANGED, CredentialEventMapper.toData(updated)));
        assertEquals(updated, credentials.findByUserId(user.getId()).orElseThrow());
        assertEquals(1, credentials.findAll().size());
    }

    @Test
    void shouldRejectPasswordChangeWithoutExistingCredential() {
        users.save(user);
        Credential updated = new Credential(user.getId(), "{bcrypt}new-hash");
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.PASSWORD_CHANGED, CredentialEventMapper.toData(updated))));
        assertTrue(credentials.findAll().isEmpty());
    }

    @Test
    void shouldRejectUnsupportedEventsAndNullDependencies() {
        assertThrows(NullPointerException.class, () -> handler.apply(null));
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event(EventType.USER_REGISTERED, Map.of())));
        assertThrows(NullPointerException.class, () -> new CredentialEventHandler(null, credentials));
        assertThrows(NullPointerException.class, () -> new CredentialEventHandler(users, null));
    }

    @Test
    void shouldRejectMalformedCredentialEvent() {
        users.save(user);
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event(EventType.CREDENTIAL_CREATED, Map.of("userId", "invalid-uuid", "passwordHash", "hash"))));
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event(EventType.CREDENTIAL_CREATED, Map.of("userId", user.getId().toString()))));
        assertTrue(credentials.findAll().isEmpty());
    }
}
