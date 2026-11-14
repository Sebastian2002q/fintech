package co.edu.uniquindio.fintech_backend.wallet.service;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.repository.WalletRepository;
import org.springframework.stereotype.Service;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class WalletService {
    private final WalletRepository repository;
    public WalletService(WalletRepository repository) {
        this.repository = Objects.requireNonNull(repository, "Wallet repository is required");
    }
    public List<Wallet> getMine(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        return repository.findByOwnerId(ownerId).stream().sorted(Comparator.comparing(Wallet::getCreatedAt).thenComparing(Wallet::getId)).toList();
    }
    public Wallet getMineById(UUID ownerId, UUID walletId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        Objects.requireNonNull(walletId, "Wallet ID is required");
        return repository.findById(walletId).filter(wallet -> wallet.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new NoSuchElementException("Wallet not found"));
    }
}
