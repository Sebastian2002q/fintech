package co.edu.uniquindio.fintech_backend.wallet.persistence;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.*;
import co.edu.uniquindio.fintech_backend.persistence.recovery.SystemRecoveryCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.*;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.persistence.event.*;
import co.edu.uniquindio.fintech_backend.user.persistence.snapshot.UserSnapshotService;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.persistence.event.WalletEventHandler;
import co.edu.uniquindio.fintech_backend.wallet.persistence.service.WalletCommandService;
import co.edu.uniquindio.fintech_backend.wallet.persistence.snapshot.WalletSnapshotService;
import co.edu.uniquindio.fintech_backend.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import tools.jackson.databind.json.JsonMapper;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletPersistenceIntegrationTest {
    @TempDir Path dir;
    private final JsonMapper mapper = JsonMapper.builder().build();
    private SnapshotCoordinator snapshots(Path path, InMemoryUserRepository users, InMemoryWalletRepository wallets) {
        return new SnapshotCoordinator(new JsonSnapshotStore(path.toString(), mapper), mapper, List.of(new UserSnapshotService(users), new WalletSnapshotService(wallets)));
    }
    private long recover(JsonlEventStore store, Path path, InMemoryUserRepository users, InMemoryWalletRepository wallets) {
        EventReplayCoordinator replay = new EventReplayCoordinator(store, List.of(new UserEventHandler(users), new WalletEventHandler(users, wallets)));
        return new SystemRecoveryCoordinator(snapshots(path, users, wallets), replay).recover();
    }
    @Test
    void shouldRestoreWalletFromCompleteEventLog() {
        Path events = dir.resolve("events.jsonl"), snapshot = dir.resolve("snapshot.json");
        UUID walletId;
        try (JsonlEventStore store = new JsonlEventStore(events.toString(), mapper)) {
            InMemoryUserRepository users = new InMemoryUserRepository(); InMemoryWalletRepository wallets = new InMemoryWalletRepository();
            User u = new User("100100", "Carlos", "Perez", "carlos@example.com");
            new EventWriter(store).record(EventType.USER_REGISTERED, UserEventMapper.toData(u)); users.save(u);
            WalletCommandService commands = new WalletCommandService(users, wallets, new EventWriter(store), new PersistenceOperationCoordinator(store, snapshots(snapshot, users, wallets)));
            Wallet w = commands.create(u.getId(), "Savings", WalletType.SAVINGS); walletId = w.getId();
            commands.update(u.getId(), walletId, "Trips", WalletType.TRANSPORT);
            assertEquals(3, store.getLastSequence());
        }
        try (JsonlEventStore store = new JsonlEventStore(events.toString(), mapper)) {
            InMemoryUserRepository users = new InMemoryUserRepository(); InMemoryWalletRepository wallets = new InMemoryWalletRepository();
            assertEquals(3, recover(store, snapshot, users, wallets));
            assertEquals("Trips", wallets.findById(walletId).orElseThrow().getName());
        }
    }
    @Test
    void shouldRestoreSnapshotAndLaterWalletEvents() {
        Path events = dir.resolve("other-events.jsonl"), snapshot = dir.resolve("other-snapshot.json");
        UUID walletId;
        try (JsonlEventStore store = new JsonlEventStore(events.toString(), mapper)) {
            InMemoryUserRepository users = new InMemoryUserRepository(); InMemoryWalletRepository wallets = new InMemoryWalletRepository();
            User u = new User("100100", "Carlos", "Perez", "carlos@example.com");
            new EventWriter(store).record(EventType.USER_REGISTERED, UserEventMapper.toData(u)); users.save(u);
            PersistenceOperationCoordinator ops = new PersistenceOperationCoordinator(store, snapshots(snapshot, users, wallets));
            WalletCommandService commands = new WalletCommandService(users, wallets, new EventWriter(store), ops);
            Wallet w = commands.create(u.getId(), "Savings", WalletType.SAVINGS); walletId = w.getId();
            ops.saveSnapshot(); commands.deactivate(u.getId(), walletId);
            assertEquals(3, store.getLastSequence());
        }
        try (JsonlEventStore store = new JsonlEventStore(events.toString(), mapper)) {
            InMemoryUserRepository users = new InMemoryUserRepository(); InMemoryWalletRepository wallets = new InMemoryWalletRepository();
            assertEquals(3, recover(store, snapshot, users, wallets));
            assertEquals(WalletStatus.INACTIVE, wallets.findById(walletId).orElseThrow().getStatus());
            assertEquals(2, new JsonSnapshotStore(snapshot.toString(), mapper).loadLatest().orElseThrow().lastSequence());
        }
    }
}
