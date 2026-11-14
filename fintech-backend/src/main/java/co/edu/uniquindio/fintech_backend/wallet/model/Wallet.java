package co.edu.uniquindio.fintech_backend.wallet.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public final class Wallet {
    private static final BigDecimal ZERO = new BigDecimal("0.00");
    private final UUID id;
    private final UUID ownerId;
    private String name;
    private WalletType type;
    private BigDecimal balance;
    private WalletStatus status;
    private final Instant createdAt;

    public Wallet(UUID ownerId, String name, WalletType type) {
        this(UUID.randomUUID(), ownerId, name, type, ZERO, WalletStatus.ACTIVE, Instant.now());
    }

    public Wallet(Wallet source) {
        this(Objects.requireNonNull(source, "Wallet is required").id, source.ownerId, source.name, source.type, source.balance, source.status, source.createdAt);
    }

    private Wallet(UUID id, UUID ownerId, String name, WalletType type, BigDecimal balance, WalletStatus status, Instant createdAt) {
        this.id = Objects.requireNonNull(id, "Wallet ID is required");
        this.ownerId = Objects.requireNonNull(ownerId, "Owner ID is required");
        this.name = requireName(name);
        this.type = Objects.requireNonNull(type, "Wallet type is required");
        this.balance = requireMoney(balance, false);
        this.status = Objects.requireNonNull(status, "Wallet status is required");
        this.createdAt = Objects.requireNonNull(createdAt, "Creation date is required");
    }

    public static Wallet restore(UUID id, UUID ownerId, String name, WalletType type, BigDecimal balance, WalletStatus status, Instant createdAt) {
        return new Wallet(id, ownerId, name, type, balance, status, createdAt);
    }

    public void updateDetails(String name, WalletType type) {
        String updatedName = requireName(name);
        WalletType updatedType = Objects.requireNonNull(type, "Wallet type is required");
        this.name = updatedName;
        this.type = updatedType;
    }

    public void credit(BigDecimal amount) {
        ensureActive();
        balance = balance.add(requireMoney(amount, true));
    }

    public void debit(BigDecimal amount) {
        ensureActive();
        BigDecimal value = requireMoney(amount, true);
        if (balance.compareTo(value) < 0) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balance = balance.subtract(value);
    }

    public void activate() {
        status = WalletStatus.ACTIVE;
    }

    public void block() {
        status = WalletStatus.BLOCKED;
    }

    public void deactivate() {
        status = WalletStatus.INACTIVE;
    }

    public boolean isActive() {
        return status == WalletStatus.ACTIVE;
    }

    private void ensureActive() {
        if (!isActive()) {
            throw new IllegalStateException("Wallet is not active");
        }
    }

    private static String requireName(String name) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Wallet name is required");
        }
        return name.strip();
    }

    private static BigDecimal requireMoney(BigDecimal amount, boolean positive) {
        Objects.requireNonNull(amount, "Amount is required");
        BigDecimal value;
        try {
            value = amount.setScale(2, RoundingMode.UNNECESSARY);
        } catch (ArithmeticException exception) {
            throw new IllegalArgumentException("Amount cannot have more than two decimal places", exception);
        }
        if (positive && value.signum() <= 0) {
            throw new IllegalArgumentException("Amount must be positive");
        }
        if (!positive && value.signum() < 0) {
            throw new IllegalArgumentException("Balance cannot be negative");
        }
        return value;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public WalletType getType() {
        return type;
    }

    public BigDecimal getBalance() {
        return balance;
    }

    public WalletStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
