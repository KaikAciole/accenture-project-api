package br.com.accenture.payment.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public interface WalletTopUpGateway {

    WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request);

    record WalletTopUpGatewayRequest(
            UUID topUpId,
            UUID walletId,
            UUID customerId,
            BigDecimal amount,
            String customerEmail
    ) {
    }

    record WalletTopUpGatewayResponse(
            String externalOrderId,
            String clientToken,
            String status,
            BigDecimal amount
    ) {
    }
}