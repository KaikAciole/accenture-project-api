package br.com.accenture.payment.api.webhook;

import br.com.accenture.payment.api.webhook.dto.MercadoPagoWebhookRequest;
import br.com.accenture.payment.application.service.wallet.WalletTopUpWebhookService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

class MercadoPagoWebhookControllerTest {

    @Test
    void receiveWebhookDelegatesToServiceAndReturnsOk() {
        FakeWalletTopUpWebhookService service = new FakeWalletTopUpWebhookService();
        MercadoPagoWebhookController controller = new MercadoPagoWebhookController(service);
        MercadoPagoWebhookRequest request = new MercadoPagoWebhookRequest(
                "payment.updated",
                "order",
                new MercadoPagoWebhookRequest.MercadoPagoWebhookData("mp-order-1")
        );

        ResponseEntity<Void> response = controller.receiveWebhook(request);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(service.lastRequest).isEqualTo(request);
    }

    private static final class FakeWalletTopUpWebhookService extends WalletTopUpWebhookService {

        private MercadoPagoWebhookRequest lastRequest;

        private FakeWalletTopUpWebhookService() {
            super(null, null);
        }

        @Override
        public void processOrderNotification(MercadoPagoWebhookRequest request) {
            this.lastRequest = request;
        }
    }
}
