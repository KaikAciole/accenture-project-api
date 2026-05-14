package br.com.accenture.payment.domain.wallet.repository;

import br.com.accenture.payment.domain.wallet.model.WalletTopUp;

import java.util.Optional;
import java.util.UUID;

public interface WalletTopUpRepository {

    WalletTopUp save(WalletTopUp walletTopUp);

    Optional<WalletTopUp> findById(UUID id);

    Optional<WalletTopUp> findByExternalOrderId(String externalOrderId);
}