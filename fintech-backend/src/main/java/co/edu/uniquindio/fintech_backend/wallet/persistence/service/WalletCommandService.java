package co.edu.uniquindio.fintech_backend.wallet.persistence.service;

import co.edu.uniquindio.fintech_backend.persistence.coordination.PersistenceOperationCoordinator;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.EventWriter;
import co.edu.uniquindio.fintech_backend.user.model.User;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletStatus;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;
import co.edu.uniquindio.fintech_backend.wallet.persistence.event.*;
import co.edu.uniquindio.fintech_backend.wallet.repository.WalletRepository;
import co.edu.uniquindio.fintech_backend.wallet.service.WalletCommands;

import org.springframework.stereotype.Service;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.UUID;

@Service
public class WalletCommandService implements WalletCommands {
    private final UserRepository users;
    private final WalletRepository wallets;
    private final EventWriter writer;
    private final PersistenceOperationCoordinator operations;

    public WalletCommandService(UserRepository users, WalletRepository wallets, EventWriter writer, PersistenceOperationCoordinator operations) {
        this.users = Objects.requireNonNull(users, "User repository is required");
        this.wallets = Objects.requireNonNull(wallets, "Wallet repository is required");
        this.writer = Objects.requireNonNull(writer, "Event writer is required");
        this.operations = Objects.requireNonNull(operations, "Persistence coordinator is required");
    }

    @Override
    public Wallet create(UUID ownerId, String name, WalletType type) {
        return operations.execute(() -> {
            requireActiveOwner(ownerId);
            Wallet wallet = new Wallet(ownerId, name, type);
            persist(EventType.WALLET_CREATED, wallet);
            return new Wallet(wallet);
        });
    }

    @Override
    public Wallet update(UUID ownerId, UUID walletId, String name, WalletType type) {
        return operations.execute(() -> {
            requireActiveOwner(ownerId);
            Wallet wallet = findOwned(ownerId, walletId);
            if (!wallet.isActive()) throw new IllegalStateException("Wallet must be active");
            wallet.updateDetails(name, type);
            persist(EventType.WALLET_UPDATED, wallet);
            return new Wallet(wallet);
        });
    }

    @Override
    public Wallet deactivate(UUID ownerId, UUID walletId) {
        return operations.execute(() -> {
            requireActiveOwner(ownerId);
            Wallet wallet = findOwned(ownerId, walletId);
            if (wallet.getStatus() == WalletStatus.INACTIVE) return wallet;
            if (wallet.getBalance().signum() != 0) throw new IllegalStateException("Wallet balance must be zero");
            wallet.deactivate();
            persist(EventType.WALLET_DEACTIVATED, wallet);
            return new Wallet(wallet);
        });
    }

    @Override
    public Wallet activate(UUID ownerId, UUID walletId) {
        return operations.execute(() -> {
            requireActiveOwner(ownerId);
            Wallet wallet = findOwned(ownerId, walletId);
            if (wallet.getStatus() == WalletStatus.ACTIVE) return wallet;
            if (wallet.getStatus() == WalletStatus.BLOCKED)
                throw new IllegalStateException("Blocked wallet cannot be activated by owner");
            wallet.activate();
            persist(EventType.WALLET_ACTIVATED, wallet);
            return new Wallet(wallet);
        });
    }

    private Wallet findOwned(UUID ownerId, UUID walletId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        Objects.requireNonNull(walletId, "Wallet ID is required");
        return wallets.findById(walletId).filter(wallet -> wallet.getOwnerId().equals(ownerId)).orElseThrow(() -> new NoSuchElementException("Wallet not found"));
    }

    private void requireActiveOwner(UUID ownerId) {
        Objects.requireNonNull(ownerId, "Owner ID is required");
        User owner = users.findById(ownerId).orElseThrow(() -> new NoSuchElementException("User not found"));
        if (!owner.isActive()) throw new IllegalStateException("User is not active");
    }

    private void persist(EventType type, Wallet wallet) {
        writer.record(type, WalletEventMapper.toData(wallet));
        wallets.save(wallet);
    }
}
