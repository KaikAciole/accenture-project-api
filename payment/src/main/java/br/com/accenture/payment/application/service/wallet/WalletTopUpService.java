package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletTopUpService {

    private final WalletService walletService;
    private final WalletTopUpRepository walletTopUpRepository;
    private final WalletTopUpGateway walletTopUpGateway;

    public WalletTopUpService(
            WalletService walletService,
            WalletTopUpRepository walletTopUpRepository,
            WalletTopUpGateway walletTopUpGateway
    ) {
        this.walletService = walletService;
        this.walletTopUpRepository = walletTopUpRepository;
        this.walletTopUpGateway = walletTopUpGateway;
    }

    @Transactional
    public WalletTopUp startTopUp(
            UUID walletId,
            UUID customerId,
            BigDecimal amount,
            String customerEmail
    ) {
        Wallet wallet = walletService.findById(walletId);

        WalletTopUp topUp = WalletTopUp.createNew(
                wallet.getId(),
                customerId,
                amount
        );

        WalletTopUp savedTopUp = walletTopUpRepository.save(topUp);

        WalletTopUpGateway.WalletTopUpGatewayResponse gatewayResponse = walletTopUpGateway.createOrder(
                new WalletTopUpGateway.WalletTopUpGatewayRequest(
                        savedTopUp.getId(),
                        savedTopUp.getWalletId(),
                        savedTopUp.getCustomerId(),
                        savedTopUp.getAmount(),
                        customerEmail
                )
        );

        savedTopUp.attachExternalOrder(
                gatewayResponse.externalOrderId(),
                gatewayResponse.clientToken()
        );

        return walletTopUpRepository.save(savedTopUp);
    }
}