package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletTopUpService {

    private final WalletTopUpTransactionService transactionService;
    private final WalletTopUpGateway walletTopUpGateway;
    private final WalletTopUpRepository walletTopUpRepository;

    public WalletTopUpService(
            WalletTopUpTransactionService transactionService,
            WalletTopUpGateway walletTopUpGateway,
            WalletTopUpRepository walletTopUpRepository
    ) {
        this.transactionService = transactionService;
        this.walletTopUpGateway = walletTopUpGateway;
        this.walletTopUpRepository = walletTopUpRepository;
    }

    public WalletTopUp createPendingTopUp(
            UUID walletId,
            UUID customerId,
            BigDecimal amount
    ) {
        return transactionService.createPendingTopUp(walletId, customerId, amount);
    }

    public TopUpSubmissionResult submitToMercadoPago(UUID topUpId) {
        WalletTopUp topUp = walletTopUpRepository.findById(topUpId)
                .orElseThrow(() -> new IllegalArgumentException("Wallet top-up not found: " + topUpId));

        WalletTopUpGateway.WalletTopUpGatewayResponse gatewayResponse = walletTopUpGateway.createOrder(
                new WalletTopUpGateway.WalletTopUpGatewayRequest(
                        topUp.getId(),
                        topUp.getWalletId(),
                        topUp.getCustomerId(),
                        topUp.getAmount(),
                        null
                )
        );

        WalletTopUp updated = transactionService.attachExternalOrder(
                topUp.getId(),
                gatewayResponse.externalOrderId(),
                gatewayResponse.clientToken()
        );

        return new TopUpSubmissionResult(
                updated,
                gatewayResponse.qrCode(),
                gatewayResponse.qrCodeBase64(),
                gatewayResponse.ticketUrl()
        );
    }

    public record TopUpSubmissionResult(
            WalletTopUp topUp,
            String qrCode,
            String qrCodeBase64,
            String ticketUrl
    ) {
    }
}
