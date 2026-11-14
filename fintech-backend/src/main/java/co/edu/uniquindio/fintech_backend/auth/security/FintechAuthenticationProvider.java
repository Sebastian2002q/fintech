package co.edu.uniquindio.fintech_backend.auth.security;

import co.edu.uniquindio.fintech_backend.auth.service.AuthenticationService;
import co.edu.uniquindio.fintech_backend.auth.service.InvalidCredentialsException;
import co.edu.uniquindio.fintech_backend.user.model.User;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class FintechAuthenticationProvider implements AuthenticationProvider {
    private final AuthenticationService authenticationService;

    public FintechAuthenticationProvider(AuthenticationService authenticationService) {
        this.authenticationService = Objects.requireNonNull(authenticationService, "Authentication service is required");
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        if (authentication == null) {
            throw new BadCredentialsException("Invalid credentials");
        }
        if (!supports(authentication.getClass())) {
            return null;
        }
        if (!(authentication.getCredentials() instanceof String password)) {
            throw new BadCredentialsException("Invalid credentials");
        }
        try {
            User user = authenticationService.authenticate(authentication.getName(), password);
            return UsernamePasswordAuthenticationToken.authenticated(user.getId().toString(), null, List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())));
        } catch (InvalidCredentialsException exception) {
            throw new BadCredentialsException("Invalid credentials");
        }
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
