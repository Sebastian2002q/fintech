package co.edu.uniquindio.fintech_backend.wallet.controller;

import co.edu.uniquindio.fintech_backend.wallet.dto.WalletRequest;
import co.edu.uniquindio.fintech_backend.wallet.dto.WalletResponse;
import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.service.WalletCommands;
import co.edu.uniquindio.fintech_backend.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@RestController
@RequestMapping("/api/wallets")
public class WalletController {
    private final WalletCommands commands;
    private final WalletService service;

    public WalletController(WalletCommands commands, WalletService service) {
        this.commands = Objects.requireNonNull(commands, "Wallet commands are required");
        this.service = Objects.requireNonNull(service, "Wallet service is required");
    }

    @GetMapping
    public List<WalletResponse> getMine(Authentication authentication) {
        return service.getMine(ownerId(authentication)).stream().map(WalletResponse::from).toList();
    }

    @GetMapping("/{id}")
    public WalletResponse getMineById(@PathVariable UUID id, Authentication authentication) {
        return WalletResponse.from(service.getMineById(ownerId(authentication), id));
    }

    @PostMapping
    public ResponseEntity<WalletResponse> create(@Valid @RequestBody WalletRequest request, Authentication authentication) {
        Wallet wallet = commands.create(ownerId(authentication), request.name(), request.type());
        return ResponseEntity.created(URI.create("/api/wallets/" + wallet.getId())).body(WalletResponse.from(wallet));
    }

    @PutMapping("/{id}")
    public WalletResponse update(@PathVariable UUID id, @Valid @RequestBody WalletRequest request, Authentication authentication) {
        return WalletResponse.from(commands.update(ownerId(authentication), id, request.name(), request.type()));
    }

    @PatchMapping("/{id}/deactivate")
    public WalletResponse deactivate(@PathVariable UUID id, Authentication authentication) {
        return WalletResponse.from(commands.deactivate(ownerId(authentication), id));
    }

    @PatchMapping("/{id}/activate")
    public WalletResponse activate(@PathVariable UUID id, Authentication authentication) {
        return WalletResponse.from(commands.activate(ownerId(authentication), id));
    }

    private UUID ownerId(Authentication authentication) {
        return UUID.fromString(Objects.requireNonNull(authentication, "Authentication is required").getName());
    }
}
