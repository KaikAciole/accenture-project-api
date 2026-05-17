package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.api.webhook.dto.AbacatePayWebhookRequest;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTopUpWebhookServiceTest {

    private static final UUID TOP_UP_ID = UUID.fromString("1779bfb5-b06a-445e-a57d-f6b5e5701770");

    @Test
    void processNotificationApprovesTopUpWhenEventIsCompletedAndExternalIdIsAValidUuid() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                8000,
                TOP_UP_ID.toString(),
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).hasSize(1);
        assertThat(transactionService.approveCalls.getFirst().topUpId()).isEqualTo(TOP_UP_ID);
        assertThat(transactionService.approveCalls.getFirst().paidAmount()).isEqualByComparingTo("80.00");
    }

    @Test
    void processNotificationFallsBackToMetadataTopUpIdWhenExternalIdIsBlank() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("topUpId", TOP_UP_ID.toString());
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                12345,
                "",
                metadata
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).hasSize(1);
        assertThat(transactionService.approveCalls.getFirst().topUpId()).isEqualTo(TOP_UP_ID);
        assertThat(transactionService.approveCalls.getFirst().paidAmount()).isEqualByComparingTo("123.45");
    }

    @Test
    void processNotificationMatchesEventCaseInsensitively() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "TRANSPARENT.COMPLETED",
                "bill-1",
                500,
                TOP_UP_ID.toString(),
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).hasSize(1);
        assertThat(transactionService.approveCalls.getFirst().paidAmount()).isEqualByComparingTo("5.00");
    }

    @Test
    void processNotificationConvertsNullAmountToZero() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                null,
                TOP_UP_ID.toString(),
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).hasSize(1);
        assertThat(transactionService.approveCalls.getFirst().paidAmount()).isEqualByComparingTo("0");
    }

    @Test
    void processNotificationIgnoresNullRequest() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);

        service.processNotification(null);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresRequestWithoutData() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = new AbacatePayWebhookRequest("id", "transparent.completed", 1, false, null);

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresRequestWithoutTransparent() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = new AbacatePayWebhookRequest(
                "id",
                "transparent.completed",
                1,
                false,
                new AbacatePayWebhookRequest.Data(null)
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresUnknownEvents() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.refused",
                "bill-1",
                8000,
                TOP_UP_ID.toString(),
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresPayloadWithoutAnyIdentifier() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                8000,
                null,
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresPayloadWithMetadataMissingTopUpId() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("other", "value");
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                8000,
                "",
                metadata
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    @Test
    void processNotificationIgnoresInvalidUuid() {
        FakeWalletTopUpTransactionService transactionService = new FakeWalletTopUpTransactionService();
        WalletTopUpWebhookService service = new WalletTopUpWebhookService(transactionService);
        AbacatePayWebhookRequest request = webhookRequest(
                "transparent.completed",
                "bill-1",
                8000,
                "not-a-uuid",
                null
        );

        service.processNotification(request);

        assertThat(transactionService.approveCalls).isEmpty();
    }

    private static AbacatePayWebhookRequest webhookRequest(
            String event,
            String billId,
            Integer amountInCents,
            String externalId,
            Map<String, Object> metadata
    ) {
        return new AbacatePayWebhookRequest(
                "evt_1",
                event,
                1,
                false,
                new AbacatePayWebhookRequest.Data(
                        new AbacatePayWebhookRequest.Transparent(
                                billId,
                                "PAID",
                                amountInCents,
                                externalId,
                                metadata
                        )
                )
        );
    }

    private record ApproveCall(UUID topUpId, BigDecimal paidAmount) {
    }

    private static final class FakeWalletTopUpTransactionService extends WalletTopUpTransactionService {

        private final java.util.List<ApproveCall> approveCalls = new java.util.ArrayList<>();

        private FakeWalletTopUpTransactionService() {
            super(null, null, BigDecimal.ZERO);
        }

        @Override
        public void approveTopUpAndCreditWallet(UUID topUpId, BigDecimal paidAmount) {
            approveCalls.add(new ApproveCall(topUpId, paidAmount));
        }
    }
}
