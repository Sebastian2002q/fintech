package co.edu.uniquindio.fintech_backend.auth.security;

import co.edu.uniquindio.fintech_backend.auth.service.AuthenticationService;
import co.edu.uniquindio.fintech_backend.auth.service.InvalidCredentialsException;
import co.edu.uniquindio.fintech_backend.user.model.Role;
import co.edu.uniquindio.fintech_backend.user.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FintechAuthenticationProviderTest {
    private static final String EMAIL = "carlos@gmail.com";
    private static final String PASSWORD = "MySecurePassword2026!";
    private final AuthenticationService authenticationService = mock(AuthenticationService.class);
    private final FintechAuthenticationProvider provider = new FintechAuthenticationProvider(authenticationService);
    private final User user = new User("1090123456", "Carlos", "Perez", EMAIL);

    private Authentication request() {
        return UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, PASSWORD);
    }

    @Test
    void shouldAuthenticateUserWithoutKeepingPassword() {
        // Arrange
        when(authenticationService.authenticate(EMAIL, PASSWORD)).thenReturn(user);
        // Act
        Authentication result = provider.authenticate(request());
        // Assert
        assertNotNull(result);
        assertTrue(result.isAuthenticated());
        assertEquals(user.getId().toString(), result.getName());
        assertNull(result.getCredentials());
        assertTrue(result.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_USER")));
        verify(authenticationService).authenticate(EMAIL, PASSWORD);
    }

    @Test
    void shouldAssignAdminAuthority() {
        // Arrange
        user.changeRole(Role.ADMIN);
        when(authenticationService.authenticate(EMAIL, PASSWORD)).thenReturn(user);
        // Act
        Authentication result = provider.authenticate(request());
        // Assert
        assertNotNull(result);
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), List.copyOf(result.getAuthorities()));
    }

    @Test
    void shouldAssignSuperAdminAuthority() {
        // Arrange
        user.changeRole(Role.SUPERADMIN);
        when(authenticationService.authenticate(EMAIL, PASSWORD)).thenReturn(user);
        // Act
        Authentication result = provider.authenticate(request());
        // Assert
        assertNotNull(result);
        assertEquals(List.of(new SimpleGrantedAuthority("ROLE_SUPERADMIN")), List.copyOf(result.getAuthorities()));
    }

    @Test
    void shouldRejectInvalidCredentials() {
        // Arrange
        when(authenticationService.authenticate(EMAIL, PASSWORD)).thenThrow(new InvalidCredentialsException());
        // Act & Assert
        BadCredentialsException exception = assertThrows(BadCredentialsException.class, () -> provider.authenticate(request()));
        assertEquals("Invalid credentials", exception.getMessage());
    }

    @Test
    void shouldRejectMissingPassword() {
        // Arrange
        Authentication request = UsernamePasswordAuthenticationToken.unauthenticated(EMAIL, null);
        // Act & Assert
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(request));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void shouldIgnoreUnsupportedAuthenticationTypes() {
        // Arrange
        Authentication unsupported = new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        // Act & Assert
        assertNull(provider.authenticate(unsupported));
        assertFalse(provider.supports(unsupported.getClass()));
        assertTrue(provider.supports(UsernamePasswordAuthenticationToken.class));
        verifyNoInteractions(authenticationService);
    }

    @Test
    void shouldRejectNullDependency() {
        assertThrows(NullPointerException.class, () -> new FintechAuthenticationProvider(null));
    }

    @Test
    void shouldRejectNullAuthentication() {
        assertThrows(BadCredentialsException.class, () -> provider.authenticate(null));
        verifyNoInteractions(authenticationService);
    }
}
