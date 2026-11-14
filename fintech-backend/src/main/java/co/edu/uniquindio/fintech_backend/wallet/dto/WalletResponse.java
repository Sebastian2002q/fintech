package co.edu.uniquindio.fintech_backend.wallet.dto;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletStatus;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record WalletResponse(UUID id, UUID ownerId, String name, WalletType type, BigDecimal balance, WalletStatus status, Instant createdAt) {
    public static WalletResponse from(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet is required");
        return new WalletResponse(wallet.getId(), wallet.getOwnerId(), wallet.getName(), wallet.getType(), wallet.getBalance(), wallet.getStatus(), wallet.getCreatedAt());
    }
}
