package co.edu.uniquindio.fintech_backend.wallet.persistence.event;

import co.edu.uniquindio.fintech_backend.persistence.event.EventHandler;
import co.edu.uniquindio.fintech_backend.persistence.event.EventType;
import co.edu.uniquindio.fintech_backend.persistence.event.StoredEvent;
import co.edu.uniquindio.fintech_backend.user.repository.UserRepository;
import co.edu.uniquindio.fintech_backend.wallet.model.Wallet;
import co.edu.uniquindio.fintech_backend.wallet.model.WalletStatus;
import co.edu.uniquindio.fintech_backend.wallet.repository.WalletRepository;
import org.springframework.stereotype.Component;
import java.util.Objects;
import java.util.Set;

@Component
public class WalletEventHandler implements EventHandler {
    private static final Set<EventType> TYPES = Set.of(EventType.WALLET_CREATED, EventType.WALLET_UPDATED, EventType.WALLET_ACTIVATED, EventType.WALLET_DEACTIVATED);
    private final UserRepository users;
    private final WalletRepository wallets;
    public WalletEventHandler(UserRepository users, WalletRepository wallets) {
        this.users = Objects.requireNonNull(users, "User repository is required");
        this.wallets = Objects.requireNonNull(wallets, "Wallet repository is required");
    }
    @Override
    public Set<EventType> supportedTypes() {
        return TYPES;
    }
    @Override
    public void apply(StoredEvent event) {
        Objects.requireNonNull(event, "Event is required");
        if (!TYPES.contains(event.type())) throw new IllegalArgumentException("Unsupported wallet event");
        Wallet wallet = WalletEventMapper.toWallet(event.data());
        if (users.findById(wallet.getOwnerId()).isEmpty()) throw new IllegalStateException("Wallet owner does not exist");
        if (event.type() == EventType.WALLET_CREATED) {
            if (wallets.findById(wallet.getId()).isPresent()) throw new IllegalStateException("Wallet already exists");
            if (wallet.getBalance().signum() != 0 || wallet.getStatus() != WalletStatus.ACTIVE) throw new IllegalStateException("Invalid initial wallet state");
        } else {
            Wallet previous = wallets.findById(wallet.getId()).orElseThrow(() -> new IllegalStateException("Wallet does not exist"));
            if (!previous.getOwnerId().equals(wallet.getOwnerId()) || !previous.getCreatedAt().equals(wallet.getCreatedAt())) {
                throw new IllegalStateException("Wallet identity cannot change");
            }
            if (previous.getBalance().compareTo(wallet.getBalance()) != 0) throw new IllegalStateException("Wallet balance cannot change through profile events");
            if (event.type() == EventType.WALLET_ACTIVATED && wallet.getStatus() != WalletStatus.ACTIVE) throw new IllegalStateException("Invalid activation event");
            if (event.type() == EventType.WALLET_DEACTIVATED && (wallet.getStatus() != WalletStatus.INACTIVE || wallet.getBalance().signum() != 0)) {
                throw new IllegalStateException("Invalid deactivation event");
            }
        }
        wallets.save(wallet);
    }
}
