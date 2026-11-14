package co.edu.uniquindio.fintech_backend.auth.repository;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryCredentialRepositoryTest {
    private final InMemoryCredentialRepository repository = new InMemoryCredentialRepository();

    @Test
    void shouldSaveAndFindCredential() {
        UUID id = UUID.randomUUID();
        Credential credential = new Credential(id, "hash-1");
        repository.save(credential);
        assertEquals(credential, repository.findByUserId(id).orElseThrow());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldUpdateCredentialForSameUser() {
        UUID id = UUID.randomUUID();
        repository.save(new Credential(id, "old-hash"));
        repository.save(new Credential(id, "new-hash"));
        assertEquals("new-hash", repository.findByUserId(id).orElseThrow().passwordHash());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldReturnEmptyForUnknownUser() {
        assertTrue(repository.findByUserId(UUID.randomUUID()).isEmpty());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectNullArguments() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> repository.save(null));
        assertThrows(NullPointerException.class, () -> repository.findByUserId(null));
        assertThrows(NullPointerException.class, () -> repository.replaceAll(null));
    }

    @Test
    void shouldReplaceAllCredentials() {
        UUID firstId = UUID.randomUUID();
        UUID secondId = UUID.randomUUID();
        repository.save(new Credential(UUID.randomUUID(), "previous"));
        repository.replaceAll(List.of(new Credential(firstId, "first-hash"), new Credential(secondId, "second-hash")));
        assertEquals(2, repository.findAll().size());
        assertEquals("first-hash", repository.findByUserId(firstId).orElseThrow().passwordHash());
        assertEquals("second-hash", repository.findByUserId(secondId).orElseThrow().passwordHash());
    }

    @Test
    void shouldRejectDuplicateCredentialsWithoutChangingRepository() {
        UUID originalId = UUID.randomUUID();
        UUID duplicateId = UUID.randomUUID();
        repository.save(new Credential(originalId, "original-hash"));
        assertThrows(IllegalArgumentException.class, () -> repository.replaceAll(List.of(new Credential(duplicateId, "first"), new Credential(duplicateId, "second"))));
        assertEquals(1, repository.findAll().size());
        assertEquals("original-hash", repository.findByUserId(originalId).orElseThrow().passwordHash());
        assertTrue(repository.findByUserId(duplicateId).isEmpty());
    }

    @Test
    void shouldClearCredentialsWhenRestoringEmptyState() {
        UUID id = UUID.randomUUID();
        repository.save(new Credential(id, "hash"));
        repository.replaceAll(List.of());
        assertTrue(repository.findAll().isEmpty());
        assertTrue(repository.findByUserId(id).isEmpty());
    }
}
