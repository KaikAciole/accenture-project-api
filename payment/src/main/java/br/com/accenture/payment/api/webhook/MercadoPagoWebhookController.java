package br.com.accenture.payment.api.webhook;

import br.com.accenture.payment.api.webhook.dto.MercadoPagoWebhookRequest;
import br.com.accenture.payment.application.service.wallet.WalletTopUpWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/mercado-pago")
@Tag(name = "Mercado Pago Webhooks", description = "Webhooks para confirmação de pagamentos do Mercado Pago")
public class MercadoPagoWebhookController {

    private final WalletTopUpWebhookService walletTopUpWebhookService;

    public MercadoPagoWebhookController(WalletTopUpWebhookService walletTopUpWebhookService) {
        this.walletTopUpWebhookService = walletTopUpWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody MercadoPagoWebhookRequest request) {
        walletTopUpWebhookService.processOrderNotification(request);
        return ResponseEntity.ok().build();
    }
}