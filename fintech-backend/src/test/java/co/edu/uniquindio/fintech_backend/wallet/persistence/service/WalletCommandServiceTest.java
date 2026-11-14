package co.edu.uniquindio.fintech_backend.wallet.persistence.service;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.*;
import co.edu.uniquindio.fintech_backend.persistence.snapshot.SnapshotCoordinator;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.InMemoryUserRepository;
import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WalletCommandServiceTest {
    private final InMemoryUserRepository users = new InMemoryUserRepository();
    private final InMemoryWalletRepository wallets = new InMemoryWalletRepository();
    private final EventStore store = mock(EventStore.class);
    private final WalletCommandService commands = new WalletCommandService(users, wallets, new EventWriter(store), new PersistenceOperationCoordinator(store, mock(SnapshotCoordinator.class)));
    private final User user = new User("100100", "Carlos", "Perez", "carlos@example.com");
    @Test
    void shouldCreateWalletAtZeroAndPersistEvent() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        assertEquals(user.getId(), w.getOwnerId());
        assertEquals(new BigDecimal("0.00"), w.getBalance());
        assertEquals(w.getId(), wallets.findById(w.getId()).orElseThrow().getId());
        verify(store).append(argThat(event -> event.type() == EventType.WALLET_CREATED));
    }
    @Test
    void shouldUpdateAndPersistWithoutChangingBalance() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        Wallet edited = commands.update(user.getId(), w.getId(), "Transport", WalletType.TRANSPORT);
        assertEquals("Transport", edited.getName());
        assertEquals(w.getId(), edited.getId());
        verify(store).append(argThat(event -> event.type() == EventType.WALLET_UPDATED));
    }
    @Test
    void shouldDeactivateAndActivateUsingEvents() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        assertEquals(WalletStatus.INACTIVE, commands.deactivate(user.getId(), w.getId()).getStatus());
        assertEquals(WalletStatus.ACTIVE, commands.activate(user.getId(), w.getId()).getStatus());
        verify(store).append(argThat(event -> event.type() == EventType.WALLET_DEACTIVATED));
        verify(store).append(argThat(event -> event.type() == EventType.WALLET_ACTIVATED));
    }
    @Test
    void shouldNotWriteRepeatedStatusChanges() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        commands.activate(user.getId(), w.getId()); commands.deactivate(user.getId(), w.getId()); commands.deactivate(user.getId(), w.getId());
        verify(store, times(2)).append(any(StoredEvent.class));
    }
    @Test
    void shouldHideOtherOwnersWallets() {
        users.save(user); User other = new User("200200", "Maria", "Gomez", "maria@example.com"); users.save(other);
        Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        assertThrows(NoSuchElementException.class, () -> commands.update(other.getId(), w.getId(), "Mine", WalletType.SHOPPING));
        assertThrows(NoSuchElementException.class, () -> commands.deactivate(other.getId(), w.getId()));
        verify(store, times(1)).append(any(StoredEvent.class));
    }
    @Test
    void shouldRejectInactiveOwnerAndUnknownOwner() {
        assertThrows(NoSuchElementException.class, () -> commands.create(UUID.randomUUID(), "Savings", WalletType.SAVINGS));
        user.block(); users.save(user);
        assertThrows(IllegalStateException.class, () -> commands.create(user.getId(), "Savings", WalletType.SAVINGS));
        verify(store, never()).append(any(StoredEvent.class));
    }
    @Test
    void shouldRejectDeactivatingWalletWithBalance() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        Wallet balanceWallet = wallets.findById(w.getId()).orElseThrow(); balanceWallet.credit(new BigDecimal("30.00")); wallets.save(balanceWallet);
        assertThrows(IllegalStateException.class, () -> commands.deactivate(user.getId(), w.getId()));
        assertEquals(WalletStatus.ACTIVE, wallets.findById(w.getId()).orElseThrow().getStatus());
    }
    @Test
    void shouldNotSaveWhenEventWriteFails() {
        users.save(user); doThrow(new IllegalStateException("Write failed")).when(store).append(any(StoredEvent.class));
        assertThrows(IllegalStateException.class, () -> commands.create(user.getId(), "Savings", WalletType.SAVINGS));
        assertTrue(wallets.findAll().isEmpty());
    }
    @Test
    void shouldRejectBlockedWalletReactivationByOwner() {
        users.save(user); Wallet w = commands.create(user.getId(), "Savings", WalletType.SAVINGS);
        Wallet blocked = wallets.findById(w.getId()).orElseThrow(); blocked.block(); wallets.save(blocked);
        assertThrows(IllegalStateException.class, () -> commands.activate(user.getId(), w.getId()));
    }
}
