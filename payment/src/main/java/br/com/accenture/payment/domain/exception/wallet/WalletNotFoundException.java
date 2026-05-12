package br.com.accenture.payment.domain.exception.wallet;

import br.com.accenture.payment.domain.enums.wallet.WalletOwnerType;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID id) {
        super("Wallet not found with id: " + id);
    }

    public static WalletNotFoundException byOwner(UUID ownerId, WalletOwnerType ownerType) {
        return new WalletNotFoundException(
                "Wallet not found for owner id: " + ownerId + " and owner type: " + ownerType
        );
    }

    private WalletNotFoundException(String message) {
        super(message);
    }
}