package co.edu.uniquindio.fintech_backend.wallet.persistence.event;

import co.edu.uniquindio.fintech_backend.wallet.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class WalletEventMapperTest {
    @Test
    void shouldRoundTripWalletWithBalanceAndStatus() {
        Wallet w = new Wallet(UUID.randomUUID(), "Savings", WalletType.SAVINGS);
        w.credit(new BigDecimal("123.45")); w.block();
        Wallet result = WalletEventMapper.toWallet(WalletEventMapper.toData(w));
        assertEquals(w.getId(), result.getId());
        assertEquals(w.getOwnerId(), result.getOwnerId());
        assertEquals(w.getBalance(), result.getBalance());
        assertEquals(w.getStatus(), result.getStatus());
        assertEquals(w.getCreatedAt(), result.getCreatedAt());
        assertEquals(w.getName(), result.getName());
    }
    @Test
    void shouldRejectMissingDataAndNull() {
        assertThrows(NullPointerException.class, () -> WalletEventMapper.toData(null));
        assertThrows(NullPointerException.class, () -> WalletEventMapper.toWallet(null));
        Wallet w = new Wallet(UUID.randomUUID(), "Savings", WalletType.SAVINGS);
        var map = new HashMap<>(WalletEventMapper.toData(w)); map.remove("ownerId");
        assertThrows(IllegalArgumentException.class, () -> WalletEventMapper.toWallet(map));
    }
}
