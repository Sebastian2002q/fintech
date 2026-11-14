package co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotParticipant;
import co.edu.uniquindio.fintech_backend.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import java.util.Objects;

@Service
public class WalletSnapshotService implements SnapshotParticipant<WalletSnapshotState> {
    private final WalletRepository wallets;
    public WalletSnapshotService(WalletRepository wallets) {
        this.wallets = Objects.requireNonNull(wallets, "Wallet repository is required");
    }
    @Override
    public String moduleKey() {
        return "wallets";
    }
    @Override
    public Class<WalletSnapshotState> stateType() {
        return WalletSnapshotState.class;
    }
    @Override
    public WalletSnapshotState capture() {
        return new WalletSnapshotState(wallets.findAll().stream().map(WalletSnapshotMapper::toEntry).toList());
    }
    @Override
    public void restore(WalletSnapshotState state) {
        Objects.requireNonNull(state, "Snapshot state is required");
        wallets.replaceAll(state.wallets().stream().map(WalletSnapshotMapper::toWallet).toList());
    }
}
