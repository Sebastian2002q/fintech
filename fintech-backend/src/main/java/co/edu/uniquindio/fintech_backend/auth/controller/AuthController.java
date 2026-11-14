package co.edu.uniquindio.fintech_backend.auth.controller;

import co.edu.uniquindio.fintech_backend.auth.dto.AccountRegistrationRequest;
import co.edu.uniquindio.fintech_backend.auth.service.AuthCommands;
import co.edu.uniquindio.fintech_backend.user.dto.UserResponse;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.service.UserAlreadyExistsException;
import co.edu.uniquindio.fintech_backend.user.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AuthCommands authCommands;
    private final UserService userService;

    public AuthController(AuthCommands authCommands, UserService userService) {
        this.authCommands = Objects.requireNonNull(authCommands);
        this.userService = Objects.requireNonNull(userService);
    }

    @GetMapping("/csrf")
    public Map<String, String> csrf(HttpServletRequest request) {
        CsrfToken token = (CsrfToken) request.getAttribute("_csrf");
        return Map.of("headerName", token.getHeaderName(), "token", token.getToken());
    }

    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody AccountRegistrationRequest request) {
        User user = authCommands.register(request.identification(), request.firstName(), request.lastName(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user));
    }

    @GetMapping("/me")
    public UserResponse me(Authentication authentication) {
        UUID id = UUID.fromString(authentication.getName());
        return UserResponse.from(userService.getById(id));
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleDuplicate(UserAlreadyExistsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("message", exception.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleInvalidData(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
