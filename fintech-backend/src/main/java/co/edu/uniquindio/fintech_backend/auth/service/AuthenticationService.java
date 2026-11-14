package co.edu.uniquindio.fintech_backend.auth.service;

import co.edu.uniquindio.fintech_backend.auth.model.Credential;
import co.edu.uniquindio.fintech_backend.auth.repository.CredentialRepository;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Objects;

@Service
public class AuthenticationService {
    private final UserRepository userRepository;
    private final CredentialRepository credentialRepository;
    private final PasswordHasher passwordHasher;

    public AuthenticationService(UserRepository userRepository, CredentialRepository credentialRepository, PasswordHasher passwordHasher) {
        this.userRepository = Objects.requireNonNull(userRepository, "User repository is required");
        this.credentialRepository = Objects.requireNonNull(credentialRepository, "Credential repository is required");
        this.passwordHasher = Objects.requireNonNull(passwordHasher, "Password hasher is required");
    }

    public User authenticate(String email, String password) {
        if (email == null || email.isBlank() || password == null || password.isBlank()) {
            throw new InvalidCredentialsException();
        }
        String normalizedEmail = email.strip().toLowerCase(Locale.ROOT);
        User user = userRepository.findByEmail(normalizedEmail).orElseThrow(InvalidCredentialsException::new);
        Credential credential = credentialRepository.findByUserId(user.getId()).orElseThrow(InvalidCredentialsException::new);
        if (!passwordHasher.matches(password, credential.passwordHash()) || !user.isActive()) {
            throw new InvalidCredentialsException();
        }
        return user;
    }
}
