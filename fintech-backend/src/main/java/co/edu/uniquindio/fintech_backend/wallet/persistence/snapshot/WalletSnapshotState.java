package co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot;

import java.util.List;
import java.util.Objects;

public record WalletSnapshotState(List<WalletSnapshotEntry> wallets) {
    public WalletSnapshotState {
        Objects.requireNonNull(wallets, "Wallets are required");
        wallets = List.copyOf(wallets);
    }
}
