package br.com.accenture.payment.domain.wallet.repository;

import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;

import java.util.UUID;

public interface WalletTransactionRepository {

    WalletTransaction save(WalletTransaction transaction);

    PageResult<WalletTransaction> findByWalletId(UUID walletId, PageRequest pageRequest);
}