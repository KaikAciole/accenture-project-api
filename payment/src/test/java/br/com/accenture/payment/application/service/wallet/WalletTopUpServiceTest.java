package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTopUpServiceTest {

    @Test
    void startTopUpCreatesPendingTopUpCreatesGatewayOrderAndAttachesExternalOrder() {
        UUID walletId = UUID.fromString("f3a365aa-2526-473a-ab53-5357de84d5d7");
        UUID customerId = UUID.fromString("eb968749-4f94-4c03-bfba-56a9ec4ba7f5");
        UUID topUpId = UUID.fromString("f58e8ea0-2e13-4fe0-a8a1-46b26363bd45");
        BigDecimal amount = new BigDecimal("120.00");
        String email = "customer@example.com";

        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService(topUpId, walletId, customerId, amount);
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway();
        WalletTopUpService service = new WalletTopUpService(transactionService, gateway);

        WalletTopUp result = service.startTopUp(walletId, customerId, amount, email);

        assertThat(transactionService.createPendingTopUpWalletId).isEqualTo(walletId);
        assertThat(transactionService.createPendingTopUpCustomerId).isEqualTo(customerId);
        assertThat(transactionService.createPendingTopUpAmount).isEqualByComparingTo(amount);
        assertThat(gateway.capturedRequest.topUpId()).isEqualTo(topUpId);
        assertThat(gateway.capturedRequest.walletId()).isEqualTo(walletId);
        assertThat(gateway.capturedRequest.customerId()).isEqualTo(customerId);
        assertThat(gateway.capturedRequest.amount()).isEqualByComparingTo(amount);
        assertThat(gateway.capturedRequest.customerEmail()).isEqualTo(email);
        assertThat(transactionService.attachExternalOrderTopUpId).isEqualTo(topUpId);
        assertThat(transactionService.attachExternalOrderExternalOrderId).isEqualTo("mp-order-1");
        assertThat(transactionService.attachExternalOrderClientToken).isEqualTo("client-token-1");
        assertThat(result.getExternalOrderId()).isEqualTo("mp-order-1");
        assertThat(result.getClientToken()).isEqualTo("client-token-1");
    }

    private static final class FakeWalletTopUpGateway implements WalletTopUpGateway {

        private WalletTopUpGatewayRequest capturedRequest;

        @Override
        public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
            this.capturedRequest = request;
            return new WalletTopUpGatewayResponse(
                    "mp-order-1",
                    "client-token-1",
                    "pending",
                    request.amount()
            );
        }

        @Override
        public WalletTopUpOrderResponse getOrderById(String externalOrderId) {
            throw new UnsupportedOperationException("Not used in this test");
        }
    }

    private static final class FakeWalletTopUpTransactionService extends WalletTopUpTransactionService {

        private final WalletTopUp pendingTopUp;
        private UUID createPendingTopUpWalletId;
        private UUID createPendingTopUpCustomerId;
        private BigDecimal createPendingTopUpAmount;
        private UUID attachExternalOrderTopUpId;
        private String attachExternalOrderExternalOrderId;
        private String attachExternalOrderClientToken;

        private FakeWalletTopUpTransactionService(UUID topUpId, UUID walletId, UUID customerId, BigDecimal amount) {
            super(null, null);
            this.pendingTopUp = WalletTopUp.restore(
                    topUpId,
                    walletId,
                    customerId,
                    amount,
                    WalletTopUpStatus.PENDING,
                    null,
                    null,
                    Instant.now(),
                    Instant.now(),
                    null
            );
        }

        @Override
        public WalletTopUp createPendingTopUp(UUID walletId, UUID customerId, BigDecimal amount) {
            this.createPendingTopUpWalletId = walletId;
            this.createPendingTopUpCustomerId = customerId;
            this.createPendingTopUpAmount = amount;
            return pendingTopUp;
        }

        @Override
        public WalletTopUp attachExternalOrder(UUID topUpId, String externalOrderId, String clientToken) {
            this.attachExternalOrderTopUpId = topUpId;
            this.attachExternalOrderExternalOrderId = externalOrderId;
            this.attachExternalOrderClientToken = clientToken;
            pendingTopUp.attachExternalOrder(externalOrderId, clientToken);
            return pendingTopUp;
        }
    }
}
