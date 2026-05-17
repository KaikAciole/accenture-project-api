package br.com.accenture.payment.api.webhook;

import br.com.accenture.payment.api.webhook.dto.AbacatePayWebhookRequest;
import br.com.accenture.payment.application.service.wallet.WalletTopUpWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class AbacatePayWebhookControllerTest {

    @Test
    void receiveWebhookDelegatesToServiceAndReturnsOk() {
        FakeWalletTopUpWebhookService service = new FakeWalletTopUpWebhookService();
        AbacatePayWebhookController controller = new AbacatePayWebhookController(service);
        AbacatePayWebhookRequest request = new AbacatePayWebhookRequest(
                "evt_1",
                "transparent.completed",
                1,
                false,
                new AbacatePayWebhookRequest.Data(
                        new AbacatePayWebhookRequest.Transparent(
                                "bill_1",
                                "PAID",
                                8000,
                                "external-1",
                                null
                        )
                )
        );

        ResponseEntity<Void> response = controller.receiveWebhook(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(service.lastRequest).isEqualTo(request);
    }

    private static final class FakeWalletTopUpWebhookService extends WalletTopUpWebhookService {

        private AbacatePayWebhookRequest lastRequest;

        private FakeWalletTopUpWebhookService() {
            super(null);
        }

        @Override
        public void processNotification(AbacatePayWebhookRequest request) {
            this.lastRequest = request;
        }
    }
}
