package co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot;

import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletSnapshotServiceTest {
    private final InMemoryWalletRepository repo = new InMemoryWalletRepository();
    private final WalletSnapshotService service = new WalletSnapshotService(repo);
    @Test
    void shouldCaptureAndRestoreWallets() {
        Wallet w = new Wallet(UUID.randomUUID(), "Daily", WalletType.DAILY_EXPENSES);
        w.credit(new BigDecimal("99.99")); repo.save(w);
        WalletSnapshotState state = service.capture(); repo.replaceAll(List.of()); service.restore(state);
        Wallet restored = repo.findById(w.getId()).orElseThrow();
        assertEquals(w.getBalance(), restored.getBalance());
        assertEquals(w.getCreatedAt(), restored.getCreatedAt());
        assertEquals("wallets", service.moduleKey());
        assertEquals(WalletSnapshotState.class, service.stateType());
    }
    @Test
    void shouldRejectNullAndDuplicateRestore() {
        Wallet w = new Wallet(UUID.randomUUID(), "Daily", WalletType.DAILY_EXPENSES); repo.save(w);
        WalletSnapshotEntry entry = WalletSnapshotMapper.toEntry(w);
        assertThrows(NullPointerException.class, () -> service.restore(null));
        assertThrows(IllegalArgumentException.class, () -> service.restore(new WalletSnapshotState(List.of(entry, entry))));
        assertEquals(1, repo.findAll().size());
    }
    @Test
    void shouldProtectSnapshotList() {
        Wallet w = new Wallet(UUID.randomUUID(), "Daily", WalletType.DAILY_EXPENSES);
        WalletSnapshotState state = new WalletSnapshotState(List.of(WalletSnapshotMapper.toEntry(w)));
        assertThrows(UnsupportedOperationException.class, () -> state.wallets().clear());
        assertEquals(w.getId(), WalletSnapshotMapper.toWallet(state.wallets().getFirst()).getId());
        assertThrows(NullPointerException.class, () -> new WalletSnapshotState(null));
    }
}
