package co.edu.uniquindio.fintech_backend.wallet.service;

import co.edu.uniquindio.fintech_backend.wallet.model.*;
import co.edu.uniquindio.fintech_backend.wallet.repository.InMemoryWalletRepository;
import org.junit.jupiter.api.Test;
import java.util.NoSuchElementException;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletServiceTest {
    private final InMemoryWalletRepository repo = new InMemoryWalletRepository();
    private final WalletService service = new WalletService(repo);
    @Test
    void shouldListOnlyOwnersWallets() {
        UUID first = UUID.randomUUID(); UUID other = UUID.randomUUID();
        repo.save(new Wallet(first, "One", WalletType.SAVINGS));
        repo.save(new Wallet(first, "Two", WalletType.TRANSPORT));
        repo.save(new Wallet(other, "Secret", WalletType.INVESTMENT));
        assertEquals(2, service.getMine(first).size());
        assertEquals(1, service.getMine(other).size());
    }
    @Test
    void shouldHideOtherUsersWalletAsNotFound() {
        UUID owner = UUID.randomUUID(); Wallet w = new Wallet(owner, "Private", WalletType.SAVINGS); repo.save(w);
        assertThrows(NoSuchElementException.class, () -> service.getMineById(UUID.randomUUID(), w.getId()));
        assertEquals(w.getId(), service.getMineById(owner, w.getId()).getId());
    }
    @Test
    void shouldRejectMissingWalletAndNullDependencies() {
        assertThrows(NoSuchElementException.class, () -> service.getMineById(UUID.randomUUID(), UUID.randomUUID()));
        assertThrows(NullPointerException.class, () -> new WalletService(null));
        assertThrows(NullPointerException.class, () -> service.getMine(null));
    }
}
