package co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import java.util.Objects;

public final class WalletSnapshotMapper {
    private WalletSnapshotMapper() {
    }
    public static WalletSnapshotEntry toEntry(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet is required");
        return new WalletSnapshotEntry(wallet.getId(), wallet.getOwnerId(), wallet.getName(), wallet.getType(), wallet.getBalance(), wallet.getStatus(), wallet.getCreatedAt());
    }
    public static Wallet toWallet(WalletSnapshotEntry entry) {
        Objects.requireNonNull(entry, "Snapshot entry is required");
        return Wallet.restore(entry.id(), entry.ownerId(), entry.name(), entry.type(), entry.balance(), entry.status(), entry.createdAt());
    }
}
