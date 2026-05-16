package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.api.webhook.dto.MercadoPagoWebhookRequest;
import br.com.accenture.payment.application.port.WalletTopUpGateway;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class WalletTopUpWebhookService {

    private static final String ORDER_TYPE = "order";
    private static final String PROCESSED_STATUS = "processed";
    private static final String ACCREDITED_STATUS_DETAIL = "accredited";

    private final WalletTopUpGateway walletTopUpGateway;
    private final WalletTopUpTransactionService transactionService;

    public WalletTopUpWebhookService(
            WalletTopUpGateway walletTopUpGateway,
            WalletTopUpTransactionService transactionService
    ) {
        this.walletTopUpGateway = walletTopUpGateway;
        this.transactionService = transactionService;
    }

    public void processOrderNotification(MercadoPagoWebhookRequest request) {
        if (request == null || request.data() == null || request.data().id() == null) {
            return;
        }

        if (!ORDER_TYPE.equalsIgnoreCase(request.type())) {
            return;
        }

        WalletTopUpGateway.WalletTopUpOrderResponse order = walletTopUpGateway.getOrderById(
                request.data().id()
        );

        if (isApproved(order)) {
            UUID topUpId = parseTopUpId(order.externalReference());
            if (topUpId == null) {
                return;
            }
            transactionService.approveTopUpAndCreditWallet(topUpId, order.totalPaidAmount());
        }
    }

    private UUID parseTopUpId(String externalReference) {
        if (externalReference == null || externalReference.isBlank()) {
            log.warn("Webhook Mercado Pago recebido sem externalReference; ignorando.");
            return null;
        }
        try {
            return UUID.fromString(externalReference);
        } catch (IllegalArgumentException ex) {
            log.warn("Webhook Mercado Pago com externalReference inválido: {}", externalReference);
            return null;
        }
    }

    private boolean isApproved(WalletTopUpGateway.WalletTopUpOrderResponse order) {
        return PROCESSED_STATUS.equalsIgnoreCase(order.status())
                && ACCREDITED_STATUS_DETAIL.equalsIgnoreCase(order.statusDetail());
    }
}