package co.edu.uniquindio.fintech_backend.auth.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PasswordHasherTest {
    private final PasswordHasher hasher = new PasswordHasher();

    @Test
    void shouldHashAndVerifyPassword() {
        String password = "MySecurePassword123!";
        String hash = hasher.hash(password);
        assertNotEquals(password, hash);
        assertTrue(hash.startsWith("{bcrypt}"));
        assertTrue(hasher.matches(password, hash));
    }

    @Test
    void shouldGenerateDifferentHashesForSamePassword() {
        String password = "MySecurePassword123!";
        String first = hasher.hash(password);
        String second = hasher.hash(password);
        assertNotEquals(first, second);
        assertTrue(hasher.matches(password, first));
        assertTrue(hasher.matches(password, second));
    }

    @Test
    void shouldRejectIncorrectPassword() {
        String hash = hasher.hash("MySecurePassword123!");
        assertFalse(hasher.matches("AnotherPassword123!", hash));
    }

    @Test
    void shouldRejectNullBlankAndShortPasswords() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(null));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash(""));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("short"));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("             "));
    }

    @Test
    void shouldRejectPasswordsExceedingBcryptByteLimit() {
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("a".repeat(73)));
        assertThrows(IllegalArgumentException.class, () -> hasher.hash("á".repeat(37)));
    }

    @Test
    void shouldRejectMissingOrUnsupportedStoredHashes() {
        assertFalse(hasher.matches(null, "{bcrypt}hash"));
        assertFalse(hasher.matches("MySecurePassword123!", null));
        assertFalse(hasher.matches("MySecurePassword123!", ""));
        assertFalse(hasher.matches("MySecurePassword123!", "{unknown}hash"));
    }

    @Test
    void shouldTreatPasswordsAsCaseSensitive() {
        String hash = hasher.hash("MySecurePassword123!");
        assertTrue(hasher.matches("MySecurePassword123!", hash));
        assertFalse(hasher.matches("mysecurepassword123!", hash));
    }
}
