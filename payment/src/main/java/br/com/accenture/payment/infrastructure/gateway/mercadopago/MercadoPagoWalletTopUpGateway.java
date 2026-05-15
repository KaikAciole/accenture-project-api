package br.com.accenture.payment.infrastructure.gateway.mercadopago;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.infrastructure.config.MercadoPagoProperties;
import br.com.accenture.payment.infrastructure.gateway.mercadopago.dto.request.MercadoPagoCreateOrderRequest;
import br.com.accenture.payment.infrastructure.gateway.mercadopago.dto.response.MercadoPagoCreateOrderResponse;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.RoundingMode;
import java.util.List;

@Component
public class MercadoPagoWalletTopUpGateway implements WalletTopUpGateway {

    private static final String CREATE_ORDER_URI = "/v1/orders";

    private final RestClient restClient;

    public MercadoPagoWalletTopUpGateway(MercadoPagoProperties properties) {
        this.restClient = RestClient.builder()
                .baseUrl("https://api.mercadopago.com")
                .defaultHeader("Authorization", "Bearer " + properties.accessToken())
                .build();
    }

    @Override
    public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
        MercadoPagoCreateOrderRequest mercadoPagoRequest = buildCreateOrderRequest(request);

        MercadoPagoCreateOrderResponse response = restClient.post()
                .uri(CREATE_ORDER_URI)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Idempotency-Key", request.topUpId().toString())
                .body(mercadoPagoRequest)
                .retrieve()
                .body(MercadoPagoCreateOrderResponse.class);

        if (response == null) {
            throw new IllegalStateException("Mercado Pago did not return a response");
        }

        return new WalletTopUpGatewayResponse(
                response.id(),
                response.clientToken(),
                response.status(),
                request.amount()
        );
    }

    private static MercadoPagoCreateOrderRequest buildCreateOrderRequest(WalletTopUpGatewayRequest request) {
        String formattedAmount = request.amount()
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();

        return new MercadoPagoCreateOrderRequest(
                "online",
                "manual",
                request.topUpId().toString(),
                formattedAmount,
                new MercadoPagoCreateOrderRequest.Payer(
                        request.customerEmail()
                ),
                List.of(
                        new MercadoPagoCreateOrderRequest.Item(
                                "Recarga de Wallet",
                                1,
                                formattedAmount
                        )
                )
        );
    }
}