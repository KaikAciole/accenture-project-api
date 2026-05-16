package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.api.webhook.dto.MercadoPagoWebhookRequest;
import br.com.accenture.payment.application.port.WalletTopUpGateway;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTopUpWebhookServiceTest {

    @Test
    void processOrderNotificationApprovesTopUpWhenOrderIsProcessedAndAccredited() {
        UUID topUpId = UUID.fromString("1779bfb5-b06a-445e-a57d-f6b5e5701770");
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway(
                new WalletTopUpGateway.WalletTopUpOrderResponse(
                        "mp-order-1",
                        topUpId.toString(),
                        "processed",
                        "accredited",
                        new BigDecimal("80.00"),
                        new BigDecimal("80.00")
                )
        );
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(gateway, transactionService);
        MercadoPagoWebhookRequest request = new MercadoPagoWebhookRequest(
                "payment.updated",
                "order",
                new MercadoPagoWebhookRequest.MercadoPagoWebhookData("mp-order-1")
        );

        service.processOrderNotification(request);

        assertThat(gateway.lastOrderIdRequested).isEqualTo("mp-order-1");
        assertThat(transactionService.approveTopUpId).isEqualTo(topUpId);
        assertThat(transactionService.approvePaidAmount).isEqualByComparingTo("80.00");
    }

    @Test
    void processOrderNotificationIgnoresInvalidPayloadOrNonOrderTypeOrNonApprovedOrder() {
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway(
                new WalletTopUpGateway.WalletTopUpOrderResponse(
                        "mp-order-2",
                        UUID.randomUUID().toString(),
                        "pending",
                        "waiting_payment",
                        new BigDecimal("80.00"),
                        BigDecimal.ZERO
                )
        );
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(gateway, transactionService);

        service.processOrderNotification(null);
        service.processOrderNotification(new MercadoPagoWebhookRequest("x", "order", null));
        service.processOrderNotification(new MercadoPagoWebhookRequest("x", "payment", new MercadoPagoWebhookRequest.MercadoPagoWebhookData("mp-order-2")));
        service.processOrderNotification(new MercadoPagoWebhookRequest("x", "order", new MercadoPagoWebhookRequest.MercadoPagoWebhookData("mp-order-2")));

        assertThat(gateway.lastOrderIdRequested).isEqualTo("mp-order-2");
        assertThat(transactionService.approveTopUpId).isNull();
        assertThat(transactionService.approvePaidAmount).isNull();
    }

    private static final class FakeWalletTopUpGateway implements WalletTopUpGateway {

        private final WalletTopUpOrderResponse orderResponse;
        private String lastOrderIdRequested;

        private FakeWalletTopUpGateway(WalletTopUpOrderResponse orderResponse) {
            this.orderResponse = orderResponse;
        }

        @Override
        public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
            throw new UnsupportedOperationException("Not used in this test");
        }

        @Override
        public WalletTopUpOrderResponse getOrderById(String externalOrderId) {
            this.lastOrderIdRequested = externalOrderId;
            return orderResponse;
        }
    }

    private static final class FakeWalletTopUpTransactionService extends WalletTopUpTransactionService {

        private UUID approveTopUpId;
        private BigDecimal approvePaidAmount;

        private FakeWalletTopUpTransactionService() {
            super(null, null);
        }

        @Override
        public void approveTopUpAndCreditWallet(UUID topUpId, BigDecimal paidAmount) {
            this.approveTopUpId = topUpId;
            this.approvePaidAmount = paidAmount;
        }
    }
}
