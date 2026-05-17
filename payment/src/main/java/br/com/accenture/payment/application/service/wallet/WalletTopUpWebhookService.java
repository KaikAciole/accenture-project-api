package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.api.webhook.dto.AbacatePayWebhookRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
public class WalletTopUpWebhookService {

    private static final String PAID_EVENT = "transparent.completed";
    private static final String METADATA_TOP_UP_ID_KEY = "topUpId";

    private final WalletTopUpTransactionService transactionService;

    public WalletTopUpWebhookService(WalletTopUpTransactionService transactionService) {
        this.transactionService = transactionService;
    }

    public void processNotification(AbacatePayWebhookRequest request) {
        if (request == null || request.data() == null || request.data().transparent() == null) {
            return;
        }

        if (!PAID_EVENT.equalsIgnoreCase(request.event())) {
            return;
        }

        AbacatePayWebhookRequest.Transparent transparent = request.data().transparent();

        UUID topUpId = resolveTopUpId(transparent);
        if (topUpId == null) {
            return;
        }

        BigDecimal paidAmount = centsToAmount(transparent.amount());

        transactionService.approveTopUpAndCreditWallet(topUpId, paidAmount);
    }

    private UUID resolveTopUpId(AbacatePayWebhookRequest.Transparent transparent) {
        String externalId = transparent.externalId();
        if (externalId == null || externalId.isBlank()) {
            externalId = extractTopUpIdFromMetadata(transparent.metadata());
        }
        if (externalId == null || externalId.isBlank()) {
            log.warn("Webhook AbacatePay sem externalId nem metadata.topUpId; ignorando.");
            return null;
        }
        try {
            return UUID.fromString(externalId);
        } catch (IllegalArgumentException ex) {
            log.warn("Webhook AbacatePay com identificador invalido: {}", externalId);
            return null;
        }
    }

    private String extractTopUpIdFromMetadata(Map<String, Object> metadata) {
        if (metadata == null) {
            return null;
        }
        Object value = metadata.get(METADATA_TOP_UP_ID_KEY);
        return value == null ? null : value.toString();
    }

    private BigDecimal centsToAmount(Integer amountInCents) {
        if (amountInCents == null) {
            return BigDecimal.ZERO;
        }
        return new BigDecimal(amountInCents).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }
}
