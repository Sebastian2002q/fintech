package co.edu.uniquindio.fintech_backend.wallet.service;

import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;

import java.util.UUID;

public interface WalletCommands {
    Wallet create(UUID ownerId, String name, WalletType type);
    Wallet update(UUID ownerId, UUID walletId, String name, WalletType type);
    Wallet deactivate(UUID ownerId, UUID walletId);
    Wallet activate(UUID ownerId, UUID walletId);
}
