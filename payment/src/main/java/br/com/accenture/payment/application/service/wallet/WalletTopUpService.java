package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletTopUpService {

    private final WalletTopUpTransactionService transactionService;
    private final WalletTopUpGateway walletTopUpGateway;

    public WalletTopUpService(
            WalletTopUpTransactionService transactionService,
            WalletTopUpGateway walletTopUpGateway
    ) {
        this.transactionService = transactionService;
        this.walletTopUpGateway = walletTopUpGateway;
    }

    public WalletTopUp startTopUp(
            UUID walletId,
            UUID customerId,
            BigDecimal amount,
            String customerEmail
    ) {
        WalletTopUp savedTopUp = transactionService.createPendingTopUp(
                walletId,
                customerId,
                amount
        );

        WalletTopUpGateway.WalletTopUpGatewayResponse gatewayResponse = walletTopUpGateway.createOrder(
                new WalletTopUpGateway.WalletTopUpGatewayRequest(
                        savedTopUp.getId(),
                        savedTopUp.getWalletId(),
                        savedTopUp.getCustomerId(),
                        savedTopUp.getAmount(),
                        customerEmail
                )
        );

        return transactionService.attachExternalOrder(
                savedTopUp.getId(),
                gatewayResponse.externalOrderId(),
                gatewayResponse.clientToken()
        );
    }
}