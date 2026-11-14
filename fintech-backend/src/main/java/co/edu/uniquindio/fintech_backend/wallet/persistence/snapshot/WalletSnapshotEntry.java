package co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.wallet.model.WalletStatus;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record WalletSnapshotEntry(UUID id, UUID ownerId, String name, WalletType type, BigDecimal balance, WalletStatus status, Instant createdAt) {
}
