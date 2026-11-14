package co.edu.uniquindio.fintech_backend.wallet.repository;

import co.edu.uniquindio.fintech_backend.wallet.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class InMemoryWalletRepositoryTest {
    private final InMemoryWalletRepository repo = new InMemoryWalletRepository();
    private final UUID owner = UUID.randomUUID();
    private Wallet wallet() { return new Wallet(owner, "Ahorros", WalletType.SAVINGS); }
    @Test
    void shouldSaveAndFindById() {
        Wallet w = wallet(); repo.save(w);
        assertEquals(w.getId(), repo.findById(w.getId()).orElseThrow().getId());
        assertEquals(1, repo.findAll().size());
    }
    @Test
    void shouldIndexOnlyOwnersWallets() {
        Wallet first = wallet(); Wallet second = wallet(); Wallet other = new Wallet(UUID.randomUUID(), "Compras", WalletType.SHOPPING);
        repo.save(first); repo.save(second); repo.save(other);
        assertEquals(2, repo.findByOwnerId(owner).size());
        assertEquals(1, repo.findByOwnerId(other.getOwnerId()).size());
        assertTrue(repo.findByOwnerId(UUID.randomUUID()).isEmpty());
    }
    @Test
    void shouldUpdateWithoutDuplicatingIndex() {
        Wallet w = wallet(); repo.save(w); w.updateDetails("Personal", WalletType.DAILY_EXPENSES); repo.save(w);
        assertEquals(1, repo.findByOwnerId(owner).size());
        assertEquals("Personal", repo.findById(w.getId()).orElseThrow().getName());
    }
    @Test
    void shouldReturnDefensiveCopies() {
        Wallet w = wallet(); repo.save(w); w.updateDetails("Changed outside", WalletType.SHOPPING);
        assertEquals("Ahorros", repo.findById(w.getId()).orElseThrow().getName());
        Wallet returned = repo.findById(w.getId()).orElseThrow(); returned.credit(new BigDecimal("20.00"));
        assertEquals(new BigDecimal("0.00"), repo.findById(w.getId()).orElseThrow().getBalance());
    }
    @Test
    void shouldReplaceAllAndRebuildOwners() {
        Wallet previous = wallet(); repo.save(previous);
        Wallet first = wallet(); Wallet second = new Wallet(UUID.randomUUID(), "Movilidad", WalletType.TRANSPORT);
        repo.replaceAll(List.of(first, second));
        assertTrue(repo.findById(previous.getId()).isEmpty());
        assertEquals(2, repo.findAll().size());
        assertEquals(1, repo.findByOwnerId(owner).size());
        assertEquals(1, repo.findByOwnerId(second.getOwnerId()).size());
    }
    @Test
    void shouldRejectDuplicateRestoreAtomically() {
        Wallet previous = wallet(); repo.save(previous);
        Wallet duplicated = wallet();
        assertThrows(IllegalArgumentException.class, () -> repo.replaceAll(List.of(duplicated, new Wallet(duplicated))));
        assertEquals(1, repo.findAll().size());
        assertTrue(repo.findById(previous.getId()).isPresent());
    }
    @Test
    void shouldRejectOwnerChangesForSameWalletId() {
        Wallet w = wallet(); repo.save(w);
        Wallet changed = Wallet.restore(w.getId(), UUID.randomUUID(), w.getName(), w.getType(), w.getBalance(), w.getStatus(), w.getCreatedAt());
        assertThrows(IllegalArgumentException.class, () -> repo.save(changed));
        assertEquals(owner, repo.findById(w.getId()).orElseThrow().getOwnerId());
    }
    @Test
    void shouldRejectNullArguments() {
        assertThrows(NullPointerException.class, () -> repo.save(null));
        assertThrows(NullPointerException.class, () -> repo.findById(null));
        assertThrows(NullPointerException.class, () -> repo.findByOwnerId(null));
        assertThrows(NullPointerException.class, () -> repo.replaceAll(null));
    }
}
