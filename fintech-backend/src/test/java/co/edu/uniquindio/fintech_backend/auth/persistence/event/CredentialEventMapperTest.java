package co.edu.uniquindio.fintech_backend.auth.persistence.event;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialEventMapperTest {
    @Test
    void shouldConvertCredentialToEventData() {
        Credential credential = new Credential(UUID.randomUUID(), "{bcrypt}encoded-hash");
        Map<String, String> data = CredentialEventMapper.toData(credential);
        assertEquals(2, data.size());
        assertEquals(credential.userId().toString(), data.get("userId"));
        assertEquals(credential.passwordHash(), data.get("passwordHash"));
    }

    @Test
    void shouldRestoreCredentialFromEventData() {
        Credential original = new Credential(UUID.randomUUID(), "{bcrypt}encoded-hash");
        Credential restored = CredentialEventMapper.toCredential(CredentialEventMapper.toData(original));
        assertEquals(original, restored);
    }

    @Test
    void shouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> CredentialEventMapper.toData(null));
        assertThrows(NullPointerException.class, () -> CredentialEventMapper.toCredential(null));
    }

    @Test
    void shouldRejectMissingAndBlankFields() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> CredentialEventMapper.toCredential(Map.of("passwordHash", "hash")));
        assertThrows(IllegalArgumentException.class, () -> CredentialEventMapper.toCredential(Map.of("userId", id.toString())));
        assertThrows(IllegalArgumentException.class, () -> CredentialEventMapper.toCredential(Map.of("userId", id.toString(), "passwordHash", " ")));
    }

    @Test
    void shouldRejectInvalidUserId() {
        assertThrows(IllegalArgumentException.class, () -> CredentialEventMapper.toCredential(Map.of("userId", "invalid-uuid", "passwordHash", "hash")));
    }
}
