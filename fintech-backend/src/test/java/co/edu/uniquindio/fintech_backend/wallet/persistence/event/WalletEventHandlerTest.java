package co.edu.uniquindio.fintech_backend.wallet.persistence.event;

import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletEventHandlerTest {
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryWalletRepository wallets = new InMemoryWalletRepository();
    private final WalletEventHandler handler = new WalletEventHandler(users, wallets);
    private final User owner = new User("100100", "Carlos", "Perez", "carlos@example.com");
    private StoredEvent event(EventType type, Wallet wallet) {
        return new StoredEvent(1, UUID.randomUUID(), type, Instant.now(), WalletEventMapper.toData(wallet));
    }
    @Test
    void shouldReplayCreationAndUpdates() {
        users.save(owner); Wallet w = new Wallet(owner.getId(), "Savings", WalletType.SAVINGS);
        handler.apply(event(EventType.WALLET_CREATED, w));
        w.updateDetails("Travel", WalletType.TRANSPORT);
        handler.apply(event(EventType.WALLET_UPDATED, w));
        assertEquals("Travel", wallets.findById(w.getId()).orElseThrow().getName());
    }
    @Test
    void shouldReplayActivationAndDeactivation() {
        users.save(owner); Wallet w = new Wallet(owner.getId(), "Savings", WalletType.SAVINGS);
        handler.apply(event(EventType.WALLET_CREATED, w)); w.deactivate();
        handler.apply(event(EventType.WALLET_DEACTIVATED, w)); w.activate();
        handler.apply(event(EventType.WALLET_ACTIVATED, w));
        assertEquals(WalletStatus.ACTIVE, wallets.findById(w.getId()).orElseThrow().getStatus());
    }
    @Test
    void shouldRejectWalletWithMissingOwner() {
        Wallet w = new Wallet(UUID.randomUUID(), "Savings", WalletType.SAVINGS);
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.WALLET_CREATED, w)));
    }
    @Test
    void shouldRejectDuplicateAndMissingWallets() {
        users.save(owner); Wallet w = new Wallet(owner.getId(), "Savings", WalletType.SAVINGS);
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.WALLET_UPDATED, w)));
        handler.apply(event(EventType.WALLET_CREATED, w));
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.WALLET_CREATED, w)));
    }
    @Test
    void shouldRejectBalanceChangesViaProfileEvent() {
        users.save(owner); Wallet w = new Wallet(owner.getId(), "Savings", WalletType.SAVINGS);
        handler.apply(event(EventType.WALLET_CREATED, w)); w.credit(new BigDecimal("10.00"));
        assertThrows(IllegalStateException.class, () -> handler.apply(event(EventType.WALLET_UPDATED, w)));
    }
    @Test
    void shouldRejectUnsupportedEventAndNull() {
        users.save(owner); Wallet w = new Wallet(owner.getId(), "Savings", WalletType.SAVINGS);
        assertThrows(NullPointerException.class, () -> handler.apply(null));
        assertThrows(IllegalArgumentException.class, () -> handler.apply(event(EventType.USER_UPDATED, w)));
        assertTrue(handler.supportedTypes().contains(EventType.WALLET_CREATED));
    }
}
