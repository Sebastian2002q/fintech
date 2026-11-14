package co.edu.uniquindio.fintech_backend.user.controller;

import co.edu.uniquindio.fintech_backend.user.dto.UserRequest;
import co.edu.uniquindio.fintech_backend.user.dto.UserResponse;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.service.UserCommands;
import co.edu.uniquindio.fintech_backend.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final UserCommands userCommands;
    private final UserService userService;

    public UserController(UserCommands userCommands, UserService userService) {
        this.userCommands = Objects.requireNonNull(userCommands, "User commands are required");
        this.userService = Objects.requireNonNull(userService, "User service is required");
    }

    @PostMapping
    public ResponseEntity<UserResponse> register(@Valid @RequestBody UserRequest request) {
        User user = userCommands.register(request.identification(), request.firstName(), request.lastName(), request.email());
        return ResponseEntity.created(URI.create("/api/users/" + user.getId())).body(UserResponse.from(user));
    }

    @GetMapping
    public List<UserResponse> getAll() {
        return userService.getAll().stream().map(UserResponse::from).toList();
    }

    @GetMapping("/{id}")
    public UserResponse getById(@PathVariable UUID id) {
        return UserResponse.from(userService.getById(id));
    }
}
