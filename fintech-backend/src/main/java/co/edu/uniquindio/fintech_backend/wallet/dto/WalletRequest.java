package co.edu.uniquindio.fintech_backend.wallet.dto;

import co.edu.uniquindio.fintech_backend.wallet.model.WalletType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record WalletRequest(@NotBlank String name, @NotNull WalletType type) {
}
