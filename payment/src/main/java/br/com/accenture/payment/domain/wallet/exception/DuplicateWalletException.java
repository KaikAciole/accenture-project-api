package br.com.accenture.payment.domain.wallet.exception;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;

import java.util.UUID;

public class DuplicateWalletException extends RuntimeException {

    public DuplicateWalletException(UUID ownerId, WalletOwnerType ownerType) {
        super("Wallet already exists for owner id: " + ownerId + " and owner type: " + ownerType);
    }
}