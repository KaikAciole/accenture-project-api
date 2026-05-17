package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WalletTopUpServiceTest {

    private static final UUID WALLET_ID = UUID.fromString("f3a365aa-2526-473a-ab53-5357de84d5d7");
    private static final UUID CUSTOMER_ID = UUID.fromString("eb968749-4f94-4c03-bfba-56a9ec4ba7f5");
    private static final UUID TOP_UP_ID = UUID.fromString("f58e8ea0-2e13-4fe0-a8a1-46b26363bd45");
    private static final BigDecimal AMOUNT = new BigDecimal("120.00");

    @Test
    void createPendingTopUpDelegatesToTransactionService() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway();
        FakeWalletTopUpRepository repository = new FakeWalletTopUpRepository();
        WalletTopUpService service = new WalletTopUpService(transactionService, gateway, repository);

        WalletTopUp result = service.createPendingTopUp(WALLET_ID, CUSTOMER_ID, AMOUNT);

        assertThat(transactionService.createPendingCalls)
                .containsExactly(new PendingCall(WALLET_ID, CUSTOMER_ID, AMOUNT));
        assertThat(result.getId()).isEqualTo(TOP_UP_ID);
        assertThat(result.getStatus()).isEqualTo(WalletTopUpStatus.PENDING);
    }

    @Test
    void submitToMercadoPagoLoadsTopUpCallsGatewayAndAttachesExternalOrder() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway();
        gateway.response = new WalletTopUpGateway.WalletTopUpGatewayResponse(
                "abacate-1",
                null,
                "PENDING",
                AMOUNT,
                "qr-code",
                "qr-code-base64",
                "ticket-url"
        );
        FakeWalletTopUpRepository repository = new FakeWalletTopUpRepository();
        repository.findByIdResponse = Optional.of(pendingTopUp());
        WalletTopUpService service = new WalletTopUpService(transactionService, gateway, repository);

        WalletTopUpService.TopUpSubmissionResult result = service.submitToMercadoPago(TOP_UP_ID);

        assertThat(repository.findByIdCalls).containsExactly(TOP_UP_ID);
        assertThat(gateway.lastRequest.topUpId()).isEqualTo(TOP_UP_ID);
        assertThat(gateway.lastRequest.walletId()).isEqualTo(WALLET_ID);
        assertThat(gateway.lastRequest.customerId()).isEqualTo(CUSTOMER_ID);
        assertThat(gateway.lastRequest.amount()).isEqualByComparingTo(AMOUNT);
        assertThat(gateway.lastRequest.customerEmail()).isNull();
        assertThat(transactionService.attachCalls)
                .containsExactly(new AttachCall(TOP_UP_ID, "abacate-1", null));
        assertThat(result.qrCode()).isEqualTo("qr-code");
        assertThat(result.qrCodeBase64()).isEqualTo("qr-code-base64");
        assertThat(result.ticketUrl()).isEqualTo("ticket-url");
        assertThat(result.topUp().getId()).isEqualTo(TOP_UP_ID);
    }

    @Test
    void submitToMercadoPagoThrowsWhenTopUpIsMissing() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        FakeWalletTopUpGateway gateway = new FakeWalletTopUpGateway();
        FakeWalletTopUpRepository repository = new FakeWalletTopUpRepository();
        repository.findByIdResponse = Optional.empty();
        WalletTopUpService service = new WalletTopUpService(transactionService, gateway, repository);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.submitToMercadoPago(TOP_UP_ID))
                .withMessageContaining(TOP_UP_ID.toString());
        assertThat(gateway.lastRequest).isNull();
        assertThat(transactionService.attachCalls).isEmpty();
    }

    private static WalletTopUp pendingTopUp() {
        return WalletTopUp.restore(
                TOP_UP_ID,
                WALLET_ID,
                CUSTOMER_ID,
                AMOUNT,
                WalletTopUpStatus.PENDING,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private record PendingCall(UUID walletId, UUID customerId, BigDecimal amount) {
    }

    private record AttachCall(UUID topUpId, String externalOrderId, String clientToken) {
    }

    private static final class FakeWalletTopUpTransactionService extends WalletTopUpTransactionService {

        private final List<PendingCall> createPendingCalls = new ArrayList<>();
        private final List<AttachCall> attachCalls = new ArrayList<>();

        private FakeWalletTopUpTransactionService() {
            super(null, null, BigDecimal.ZERO);
        }

        @Override
        public WalletTopUp createPendingTopUp(UUID walletId, UUID customerId, BigDecimal amount) {
            createPendingCalls.add(new PendingCall(walletId, customerId, amount));
            return pendingTopUp();
        }

        @Override
        public WalletTopUp attachExternalOrder(UUID topUpId, String externalOrderId, String clientToken) {
            attachCalls.add(new AttachCall(topUpId, externalOrderId, clientToken));
            WalletTopUp topUp = pendingTopUp();
            topUp.attachExternalOrder(externalOrderId, clientToken);
            return topUp;
        }
    }

    private static final class FakeWalletTopUpGateway implements WalletTopUpGateway {

        private WalletTopUpGatewayRequest lastRequest;
        private WalletTopUpGatewayResponse response;

        @Override
        public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
            this.lastRequest = request;
            return response;
        }
    }

    private static final class FakeWalletTopUpRepository implements WalletTopUpRepository {

        private Optional<WalletTopUp> findByIdResponse = Optional.empty();
        private final List<UUID> findByIdCalls = new ArrayList<>();

        @Override
        public WalletTopUp save(WalletTopUp walletTopUp) {
            return walletTopUp;
        }

        @Override
        public Optional<WalletTopUp> findById(UUID id) {
            findByIdCalls.add(id);
            return findByIdResponse;
        }

        @Override
        public Optional<WalletTopUp> findByExternalOrderId(String externalOrderId) {
            return Optional.empty();
        }
    }
}
