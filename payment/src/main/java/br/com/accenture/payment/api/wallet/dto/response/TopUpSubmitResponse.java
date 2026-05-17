package br.com.accenture.payment.api.wallet.dto.response;

import java.util.UUID;

public record TopUpSubmitResponse(
        UUID topUpId,
        String externalOrderId,
        String status,
        String qrCode,
        String qrCodeBase64,
        String ticketUrl
) {
}
