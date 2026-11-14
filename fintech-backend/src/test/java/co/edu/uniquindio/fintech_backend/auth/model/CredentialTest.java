package co.edu.uniquindio.fintech_backend.auth.model;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class CredentialTest {
    @Test
    void shouldPreserveUserIdAndPasswordHash() {
        UUID id = UUID.randomUUID();
        Credential credential = new Credential(id, "{bcrypt}encoded-password");
        assertEquals(id, credential.userId());
        assertEquals("{bcrypt}encoded-password", credential.passwordHash());
    }

    @Test
    void shouldRejectNullUserId() {
        assertThrows(NullPointerException.class, () -> new Credential(null, "{bcrypt}encoded-password"));
    }

    @Test
    void shouldRejectMissingPasswordHash() {
        UUID id = UUID.randomUUID();
        assertThrows(IllegalArgumentException.class, () -> new Credential(id, null));
        assertThrows(IllegalArgumentException.class, () -> new Credential(id, ""));
        assertThrows(IllegalArgumentException.class, () -> new Credential(id, "   "));
    }
}
