package co.edu.uniquindio.fintech_backend.auth.service;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.InMemoryCredentialRepository;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.model.UserStatus;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AuthenticationServiceTest {
    private static final String PASSWORD = "MySecurePassword2026!";
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryCredentialRepository credentials = new InMemoryCredentialRepository();
    private final PasswordHasher hasher = new PasswordHasher();
    private final AuthenticationService service = new AuthenticationService(users, credentials, hasher);

    private User createAccount() {
        User user = new User("1090123456", "Carlos", "Perez", "carlos@gmail.com");
        users.save(user);
        credentials.save(new Credential(user.getId(), hasher.hash(PASSWORD)));
        return user;
    }

    private void assertInvalid(String email, String password) {
        InvalidCredentialsException exception = assertThrows(InvalidCredentialsException.class, () -> service.authenticate(email, password));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void shouldAuthenticateWithCorrectCredentials() {
        // Arrange
        User original = createAccount();
        // Act
        User authenticated = service.authenticate("carlos@gmail.com", PASSWORD);
        // Assert
        assertEquals(original.getId(), authenticated.getId());
        assertEquals(original.getEmail(), authenticated.getEmail());
        assertEquals(Role.USER, authenticated.getRole());
        assertEquals(UserStatus.ACTIVE, authenticated.getStatus());
    }

    @Test
    void shouldNormalizeEmailBeforeAuthentication() {
        // Arrange
        User original = createAccount();
        // Act
        User authenticated = service.authenticate("  CARLOS@GMAIL.COM  ", PASSWORD);
        // Assert
        assertEquals(original.getId(), authenticated.getId());
        assertEquals("carlos@gmail.com", authenticated.getEmail());
    }

    @Test
    void shouldRejectIncorrectPassword() {
        // Arrange
        createAccount();
        // Act & Assert
        assertInvalid("carlos@gmail.com", "IncorrectPassword2026!");
    }

    @Test
    void shouldRejectUnknownEmail() {
        // Act & Assert
        assertInvalid("unknown@gmail.com", PASSWORD);
    }

    @Test
    void shouldRejectUserWithoutCredential() {
        // Arrange
        users.save(new User("1090123456", "Carlos", "Perez", "carlos@gmail.com"));
        // Act & Assert
        assertInvalid("carlos@gmail.com", PASSWORD);
    }

    @Test
    void shouldRejectBlockedUser() {
        // Arrange
        User user = createAccount();
        user.block();
        users.save(user);
        // Act & Assert
        assertInvalid("carlos@gmail.com", PASSWORD);
        assertEquals(UserStatus.BLOCKED, users.findById(user.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectInactiveUser() {
        // Arrange
        User user = createAccount();
        user.deactivate();
        users.save(user);
        // Act & Assert
        assertInvalid("carlos@gmail.com", PASSWORD);
        assertEquals(UserStatus.INACTIVE, users.findById(user.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectNullAndBlankCredentials() {
        // Act & Assert
        assertInvalid(null, PASSWORD);
        assertInvalid("", PASSWORD);
        assertInvalid("   ", PASSWORD);
        assertInvalid("carlos@gmail.com", null);
        assertInvalid("carlos@gmail.com", "");
        assertInvalid("carlos@gmail.com", "   ");
    }

    @Test
    void shouldRejectNullDependencies() {
        // Act & Assert
        assertThrows(NullPointerException.class, () -> new AuthenticationService(null, credentials, hasher));
        assertThrows(NullPointerException.class, () -> new AuthenticationService(users, null, hasher));
        assertThrows(NullPointerException.class, () -> new AuthenticationService(users, credentials, null));
    }
}
