package co.edu.uniquindio.fintech_backend.auth.service;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class PasswordHasher {
    private final PasswordEncoder encoder = new DelegatingPasswordEncoder("bcrypt", Map.of("bcrypt", new BCryptPasswordEncoder(12)));

    public String hash(String password) {
        if (password == null || password.isBlank() || password.length() < 12) {
            throw new IllegalArgumentException("Password must contain at least 12 characters");
        }
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalArgumentException("Password cannot exceed 72 UTF-8 bytes");
        }
        return encoder.encode(password);
    }

    public boolean matches(String password, String passwordHash) {
        if (password == null || passwordHash == null || passwordHash.isBlank()) {
            return false;
        }
        try {
            return encoder.matches(password, passwordHash);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }
}
