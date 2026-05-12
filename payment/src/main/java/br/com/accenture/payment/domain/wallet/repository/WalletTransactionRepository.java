package br.com.accenture.payment.domain.wallet.repository;

import br.com.accenture.payment.domain.wallet.model.WalletTransaction;

import java.util.List;
import java.util.UUID;

public interface WalletTransactionRepository {

    WalletTransaction save(WalletTransaction transaction);

    List<WalletTransaction> findByWalletId(UUID walletId);
}