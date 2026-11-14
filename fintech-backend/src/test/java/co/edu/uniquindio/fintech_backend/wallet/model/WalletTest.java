package co.edu.uniquindio.fintech_backend.wallet.model;

import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletTest {
    private final UUID ownerId = UUID.randomUUID();
    private Wallet wallet() {
        return new Wallet(ownerId, "Daily expenses", WalletType.DAILY_EXPENSES);
    }
    @Test
    void shouldCreateActiveWalletWithZeroBalance() {
        Wallet wallet = wallet();
        assertNotNull(wallet.getId());
        assertEquals(ownerId, wallet.getOwnerId());
        assertEquals("Daily expenses", wallet.getName());
        assertEquals(WalletType.DAILY_EXPENSES, wallet.getType());
        assertEquals(new BigDecimal("0.00"), wallet.getBalance());
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertTrue(wallet.isActive());
        assertNotNull(wallet.getCreatedAt());
    }
    @Test
    void shouldAllowMultipleWalletsForSameOwner() {
        Wallet first = wallet();
        Wallet second = wallet();
        assertEquals(first.getOwnerId(), second.getOwnerId());
        assertNotEquals(first.getId(), second.getId());
    }
    @Test
    void shouldUpdateDetailsWithoutChangingIdentity() {
        Wallet wallet = wallet();
        UUID id = wallet.getId();
        Instant createdAt = wallet.getCreatedAt();
        wallet.updateDetails("  My savings  ", WalletType.SAVINGS);
        assertEquals("My savings", wallet.getName());
        assertEquals(WalletType.SAVINGS, wallet.getType());
        assertEquals(id, wallet.getId());
        assertEquals(ownerId, wallet.getOwnerId());
        assertEquals(createdAt, wallet.getCreatedAt());
    }
    @Test
    void shouldRejectInvalidCreationData() {
        assertThrows(NullPointerException.class, () -> new Wallet(null, "Savings", WalletType.SAVINGS));
        assertThrows(IllegalArgumentException.class, () -> new Wallet(ownerId, null, WalletType.SAVINGS));
        assertThrows(IllegalArgumentException.class, () -> new Wallet(ownerId, "   ", WalletType.SAVINGS));
        assertThrows(NullPointerException.class, () -> new Wallet(ownerId, "Savings", null));
    }
    @Test
    void shouldRejectInvalidUpdatesWithoutPartialModification() {
        Wallet wallet = wallet();
        assertThrows(NullPointerException.class, () -> wallet.updateDetails("New name", null));
        assertThrows(IllegalArgumentException.class, () -> wallet.updateDetails(" ", WalletType.SAVINGS));
        assertEquals("Daily expenses", wallet.getName());
        assertEquals(WalletType.DAILY_EXPENSES, wallet.getType());
    }
    @Test
    void shouldCreditWalletWithExactDecimals() {
        Wallet wallet = wallet();
        wallet.credit(new BigDecimal("0.10"));
        wallet.credit(new BigDecimal("0.20"));
        wallet.credit(new BigDecimal("5"));
        assertEquals(new BigDecimal("5.30"), wallet.getBalance());
    }
    @Test
    void shouldDebitWalletWithExactDecimals() {
        Wallet wallet = wallet();
        wallet.credit(new BigDecimal("100.25"));
        wallet.debit(new BigDecimal("40.10"));
        assertEquals(new BigDecimal("60.15"), wallet.getBalance());
        wallet.debit(new BigDecimal("60.15"));
        assertEquals(new BigDecimal("0.00"), wallet.getBalance());
    }
    @Test
    void shouldRejectInsufficientFundsWithoutChangingBalance() {
        Wallet wallet = wallet();
        wallet.credit(new BigDecimal("10.00"));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(new BigDecimal("10.01")));
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
    }
    @Test
    void shouldRejectInvalidAmountsWithoutChangingBalance() {
        Wallet wallet = wallet();
        wallet.credit(new BigDecimal("10.00"));
        assertThrows(NullPointerException.class, () -> wallet.credit(null));
        assertThrows(NullPointerException.class, () -> wallet.debit(null));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(BigDecimal.ZERO));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(new BigDecimal("-1.00")));
        assertThrows(IllegalArgumentException.class, () -> wallet.credit(new BigDecimal("1.001")));
        assertThrows(IllegalArgumentException.class, () -> wallet.debit(new BigDecimal("1.001")));
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
    }
    @Test
    void shouldRejectFinancialOperationsWhenBlockedOrInactive() {
        Wallet wallet = wallet();
        wallet.credit(new BigDecimal("10.00"));
        wallet.block();
        assertThrows(IllegalStateException.class, () -> wallet.credit(new BigDecimal("1.00")));
        assertThrows(IllegalStateException.class, () -> wallet.debit(new BigDecimal("1.00")));
        wallet.deactivate();
        assertThrows(IllegalStateException.class, () -> wallet.credit(new BigDecimal("1.00")));
        assertThrows(IllegalStateException.class, () -> wallet.debit(new BigDecimal("1.00")));
        assertEquals(new BigDecimal("10.00"), wallet.getBalance());
    }
    @Test
    void shouldManageWalletStatusTransitions() {
        Wallet wallet = wallet();
        wallet.block();
        assertEquals(WalletStatus.BLOCKED, wallet.getStatus());
        assertFalse(wallet.isActive());
        wallet.deactivate();
        assertEquals(WalletStatus.INACTIVE, wallet.getStatus());
        wallet.activate();
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertTrue(wallet.isActive());
    }
    @Test
    void shouldRestoreOriginalIdentityAndBalance() {
        UUID id = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-11-01T12:00:00Z");
        Wallet restored = Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, new BigDecimal("250.5"), WalletStatus.BLOCKED, createdAt);
        assertEquals(id, restored.getId());
        assertEquals(ownerId, restored.getOwnerId());
        assertEquals("Savings", restored.getName());
        assertEquals(WalletType.SAVINGS, restored.getType());
        assertEquals(new BigDecimal("250.50"), restored.getBalance());
        assertEquals(WalletStatus.BLOCKED, restored.getStatus());
        assertEquals(createdAt, restored.getCreatedAt());
    }
    @Test
    void shouldRejectInvalidRestoredState() {
        UUID id = UUID.randomUUID();
        Instant now = Instant.now();
        assertThrows(NullPointerException.class, () -> Wallet.restore(null, ownerId, "Savings", WalletType.SAVINGS, BigDecimal.ZERO, WalletStatus.ACTIVE, now));
        assertThrows(NullPointerException.class, () -> Wallet.restore(id, null, "Savings", WalletType.SAVINGS, BigDecimal.ZERO, WalletStatus.ACTIVE, now));
        assertThrows(IllegalArgumentException.class, () -> Wallet.restore(id, ownerId, " ", WalletType.SAVINGS, BigDecimal.ZERO, WalletStatus.ACTIVE, now));
        assertThrows(NullPointerException.class, () -> Wallet.restore(id, ownerId, "Savings", null, BigDecimal.ZERO, WalletStatus.ACTIVE, now));
        assertThrows(NullPointerException.class, () -> Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, null, WalletStatus.ACTIVE, now));
        assertThrows(NullPointerException.class, () -> Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, BigDecimal.ZERO, null, now));
        assertThrows(NullPointerException.class, () -> Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, BigDecimal.ZERO, WalletStatus.ACTIVE, null));
        assertThrows(IllegalArgumentException.class, () -> Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, new BigDecimal("-0.01"), WalletStatus.ACTIVE, now));
        assertThrows(IllegalArgumentException.class, () -> Wallet.restore(id, ownerId, "Savings", WalletType.SAVINGS, new BigDecimal("1.001"), WalletStatus.ACTIVE, now));
    }
    @Test
    void shouldCopyWalletWithoutSharingMutableState() {
        Wallet original = wallet();
        original.credit(new BigDecimal("50.00"));
        Wallet copy = new Wallet(original);
        assertNotSame(original, copy);
        assertEquals(original.getId(), copy.getId());
        assertEquals(original.getBalance(), copy.getBalance());
        copy.debit(new BigDecimal("10.00"));
        copy.updateDetails("Transport", WalletType.TRANSPORT);
        assertEquals(new BigDecimal("50.00"), original.getBalance());
        assertEquals("Daily expenses", original.getName());
        assertEquals(new BigDecimal("40.00"), copy.getBalance());
        assertThrows(NullPointerException.class, () -> new Wallet(null));
    }
}
