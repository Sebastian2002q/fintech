package co.edu.uniquindio.fintech_backend.auth.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialSnapshotServiceTest {
    private final InMemoryCredentialRepository repository = new InMemoryCredentialRepository();
    private final CredentialSnapshotService service = new CredentialSnapshotService(repository);

    @Test
    void shouldExposeModuleKeyAndStateType() {
        assertEquals("credentials", service.moduleKey());
        assertEquals(CredentialSnapshotState.class, service.stateType());
    }

    @Test
    void shouldCaptureStoredCredentials() {
        Credential first = new Credential(UUID.randomUUID(), "first-hash");
        Credential second = new Credential(UUID.randomUUID(), "second-hash");
        repository.save(first);
        repository.save(second);
        CredentialSnapshotState state = service.capture();
        assertEquals(2, state.credentials().size());
        assertTrue(state.credentials().contains(first));
        assertTrue(state.credentials().contains(second));
    }

    @Test
    void shouldRestoreAndReplaceExistingCredentials() {
        UUID oldId = UUID.randomUUID();
        Credential restored = new Credential(UUID.randomUUID(), "restored-hash");
        repository.save(new Credential(oldId, "old-hash"));
        service.restore(new CredentialSnapshotState(List.of(restored)));
        assertTrue(repository.findByUserId(oldId).isEmpty());
        assertEquals(restored, repository.findByUserId(restored.userId()).orElseThrow());
        assertEquals(1, repository.findAll().size());
    }

    @Test
    void shouldRestoreEmptySnapshot() {
        repository.save(new Credential(UUID.randomUUID(), "hash"));
        service.restore(new CredentialSnapshotState(List.of()));
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> new CredentialSnapshotService(null));
        assertThrows(NullPointerException.class, () -> service.restore(null));
    }

    @Test
    void shouldRejectDuplicateCredentialsWithoutChangingRepository() {
        Credential original = new Credential(UUID.randomUUID(), "original-hash");
        UUID duplicateId = UUID.randomUUID();
        repository.save(original);
        CredentialSnapshotState state = new CredentialSnapshotState(List.of(new Credential(duplicateId, "first-hash"), new Credential(duplicateId, "second-hash")));
        assertThrows(IllegalArgumentException.class, () -> service.restore(state));
        assertEquals(1, repository.findAll().size());
        assertEquals(original, repository.findByUserId(original.userId()).orElseThrow());
    }
}
