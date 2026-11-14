package co.edu.uniquindio.fintech_backend.wallet.repository;

import co.edu.uniquindio.fintech_backend.structures.hash.CustomHashMap;
import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import org.springframework.stereotype.Repository;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Repository
public class InMemoryWalletRepository implements WalletRepository {
    private CustomHashMap<UUID, Wallet> wallets = new CustomHashMap<>();
    private CustomHashMap<UUID, CustomHashMap<UUID, Wallet>> byOwner = new CustomHashMap<>();
    @Override
    public synchronized void save(Wallet wallet) {
        Objects.requireNonNull(wallet, "Wallet is required");
        Wallet previous = wallets.get(wallet.getId());
        if (previous != null && !previous.getOwnerId().equals(wallet.getOwnerId())) {
            throw new IllegalArgumentException("Wallet owner cannot change");
        }
        Wallet copy = new Wallet(wallet);
        wallets.put(copy.getId(), copy);
        CustomHashMap<UUID, Wallet> ownerWallets = byOwner.get(copy.getOwnerId());
        if (ownerWallets == null) {
            ownerWallets = new CustomHashMap<>();
            byOwner.put(copy.getOwnerId(), ownerWallets);
        }
        ownerWallets.put(copy.getId(), copy);
    }
    @Override
    public synchronized Optional<Wallet> findById(UUID id) {
        Objects.requireNonNull(id, "Wallet ID is required");
        Wallet wallet = wallets.get(id);
        return wallet == null ? Optional.empty() : Optional.of(new Wallet(wallet));
    }
    @Override
    public synchronized Collection<Wallet> findByOwnerId(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        CustomHashMap<UUID, Wallet> ownerWallets = byOwner.get(ownerId);
        if (ownerWallets == null) return List.of();
        List<Wallet> result = new ArrayList<>();
        ownerWallets.forEachValue(wallet -> result.add(new Wallet(wallet)));
        return List.copyOf(result);
    }
    @Override
    public synchronized Collection<Wallet> findAll() {
        List<Wallet> result = new ArrayList<>();
        wallets.forEachValue(wallet -> result.add(new Wallet(wallet)));
        return List.copyOf(result);
    }
    @Override
    public synchronized void replaceAll(Collection<Wallet> restored) {
        Objects.requireNonNull(restored, "Wallet collection is required");
        CustomHashMap<UUID, Wallet> nextWallets = new CustomHashMap<>();
        CustomHashMap<UUID, CustomHashMap<UUID, Wallet>> nextOwners = new CustomHashMap<>();
        for (Wallet wallet : restored) {
            Objects.requireNonNull(wallet, "Wallet cannot be null");
            Wallet copy = new Wallet(wallet);
            if (nextWallets.containsKey(copy.getId())) throw new IllegalArgumentException("Duplicate wallet ID");
            nextWallets.put(copy.getId(), copy);
            CustomHashMap<UUID, Wallet> ownerWallets = nextOwners.get(copy.getOwnerId());
            if (ownerWallets == null) {
                ownerWallets = new CustomHashMap<>();
                nextOwners.put(copy.getOwnerId(), ownerWallets);
            }
            ownerWallets.put(copy.getId(), copy);
        }
        wallets = nextWallets;
        byOwner = nextOwners;
    }
}
