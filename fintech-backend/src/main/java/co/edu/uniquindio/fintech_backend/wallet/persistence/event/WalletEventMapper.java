package co.edu.uniquindio.fintech_backend.wallet.persistence.event;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletStatus;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class WalletEventMapper {
    private WalletEventMapper() {
    }

    public static Map<String, String> toData(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet is required");
        return Map.of("id", wallet.getId().toString(), "ownerId", wallet.getOwnerId().toString(), "name", wallet.getName(), "type", wallet.getType().name(), "balance", wallet.getBalance().toPlainString(), "status", wallet.getStatus().name(), "createdAt", wallet.getCreatedAt().toString());
    }

    public static Wallet toWallet(Map<String, String> data) {
        Objects.requireNonNull(data, "Event data is required");
        return Wallet.restore(UUID.fromString(field(data, "id")), UUID.fromString(field(data, "ownerId")), field(data, "name"), WalletType.valueOf(field(data, "type")), new BigDecimal(field(data, "balance")), WalletStatus.valueOf(field(data, "status")), Instant.parse(field(data, "createdAt")));
    }

    private static String field(Map<String, String> data, String key) {
        String value = data.get(key);
        if (value == null || value.isBlank()) throw new IllegalArgumentException("Missing wallet field: " + key);
        return value;
    }
}
