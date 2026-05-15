package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.api.webhook.dto.MercadoPagoWebhookRequest;
import br.com.accenture.payment.application.port.WalletTopUpGateway;
import org.springframework.stereotype.Service;

import java.util.UUID;

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
            UUID topUpId = UUID.fromString(order.externalReference());
            transactionService.approveTopUpAndCreditWallet(topUpId, order.totalPaidAmount());
        }
    }

    private boolean isApproved(WalletTopUpGateway.WalletTopUpOrderResponse order) {
        return PROCESSED_STATUS.equalsIgnoreCase(order.status())
                && ACCREDITED_STATUS_DETAIL.equalsIgnoreCase(order.statusDetail());
    }
}