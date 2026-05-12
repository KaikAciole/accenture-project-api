package br.com.accenture.payment.domain.wallet.repository;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.model.Wallet;

import java.util.Optional;
import java.util.UUID;

public interface WalletRepository {

    Wallet save(Wallet wallet);

    Optional<Wallet> findById(UUID id);

    Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType);

    boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType);
}