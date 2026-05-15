package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletTopUpTransactionService {

    private final WalletService walletService;
    private final WalletTopUpRepository walletTopUpRepository;

    public WalletTopUpTransactionService(
            WalletService walletService,
            WalletTopUpRepository walletTopUpRepository
    ) {
        this.walletService = walletService;
        this.walletTopUpRepository = walletTopUpRepository;
    }

    @Transactional
    public WalletTopUp createPendingTopUp(
            UUID walletId,
            UUID customerId,
            BigDecimal amount
    ) {
        Wallet wallet = walletService.findById(walletId);

        WalletTopUp topUp = WalletTopUp.createNew(
                wallet.getId(),
                customerId,
                amount
        );

        return walletTopUpRepository.save(topUp);
    }

    @Transactional
    public WalletTopUp attachExternalOrder(
            UUID topUpId,
            String externalOrderId,
            String clientToken
    ) {
        WalletTopUp topUp = walletTopUpRepository.findById(topUpId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet top-up not found: " + topUpId));

        topUp.attachExternalOrder(externalOrderId, clientToken);

        return walletTopUpRepository.save(topUp);
    }
}