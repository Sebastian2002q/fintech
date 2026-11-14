package co.edu.uniquindio.fintech_backend.auth.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialSnapshotStateTest {
    @Test
    void shouldPreserveCredentials() {
        Credential credential = new Credential(UUID.randomUUID(), "hash");
        CredentialSnapshotState state = new CredentialSnapshotState(List.of(credential));
        assertEquals(List.of(credential), state.credentials());
    }

    @Test
    void shouldProtectCredentialsFromExternalChanges() {
        List<Credential> source = new ArrayList<>();
        source.add(new Credential(UUID.randomUUID(), "first"));
        CredentialSnapshotState state = new CredentialSnapshotState(source);
        source.clear();
        assertEquals(1, state.credentials().size());
        assertThrows(UnsupportedOperationException.class, () -> state.credentials().clear());
    }

    @Test
    void shouldRejectNullCredentials() {
        assertThrows(NullPointerException.class, () -> new CredentialSnapshotState(null));
    }
}
