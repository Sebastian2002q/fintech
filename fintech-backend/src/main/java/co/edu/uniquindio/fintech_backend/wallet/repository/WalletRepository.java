package co.edu.uniquindio.fintech_backend.wallet.repository;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {
    void save(Wallet wallet);
    Optional<Wallet> findById(UUID id);
    Collection<Wallet> findByOwnerId(UUID ownerId);
    Collection<Wallet> findAll();
    void replaceAll(Collection<Wallet> wallets);
}
