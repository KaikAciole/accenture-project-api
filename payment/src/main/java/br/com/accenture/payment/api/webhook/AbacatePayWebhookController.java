package br.com.accenture.payment.api.webhook;

import br.com.accenture.payment.api.webhook.dto.AbacatePayWebhookRequest;
import br.com.accenture.payment.application.service.wallet.WalletTopUpWebhookService;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/webhooks/abacatepay")
@Tag(name = "AbacatePay Webhooks", description = "Webhooks para confirmação de pagamentos do AbacatePay")
public class AbacatePayWebhookController {

    private final WalletTopUpWebhookService walletTopUpWebhookService;

    public AbacatePayWebhookController(WalletTopUpWebhookService walletTopUpWebhookService) {
        this.walletTopUpWebhookService = walletTopUpWebhookService;
    }

    @PostMapping
    public ResponseEntity<Void> receiveWebhook(@RequestBody AbacatePayWebhookRequest request) {
        walletTopUpWebhookService.processNotification(request);
        return ResponseEntity.ok().build();
    }
}
