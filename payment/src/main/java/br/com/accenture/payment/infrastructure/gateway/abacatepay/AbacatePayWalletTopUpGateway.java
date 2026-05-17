package br.com.accenture.payment.infrastructure.gateway.abacatepay;

import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.domain.wallet.exception.InvalidTopUpRequestException;
import br.com.accenture.payment.infrastructure.config.AbacatePayProperties;
import br.com.accenture.payment.infrastructure.gateway.abacatepay.dto.request.AbacatePayCreateBillingRequest;
import br.com.accenture.payment.infrastructure.gateway.abacatepay.dto.response.AbacatePayCreateBillingResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

@Component
public class AbacatePayWalletTopUpGateway implements WalletTopUpGateway {

    private static final String CREATE_BILLING_URI = "/v2/transparents/create";
    private static final String PIX_METHOD = "PIX";
    private static final String DESCRIPTION = "Recarga de Wallet";
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final RestClient restClient;
    private final BigDecimal fixedChargeAmount;

    @Autowired
    public AbacatePayWalletTopUpGateway(AbacatePayProperties properties) {
        this(RestClient.builder(), properties, "https://api.abacatepay.com");
    }

    AbacatePayWalletTopUpGateway(RestClient.Builder builder, AbacatePayProperties properties, String baseUrl) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
        this.fixedChargeAmount = properties.fixedChargeAmount();
    }

    @Override
    public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
        Integer amountInCents = fixedChargeAmount
                .setScale(2, RoundingMode.HALF_UP)
                .movePointRight(2)
                .intValueExact();

        AbacatePayCreateBillingRequest billingRequest = new AbacatePayCreateBillingRequest(
                PIX_METHOD,
                new AbacatePayCreateBillingRequest.Data(
                        amountInCents,
                        DESCRIPTION,
                        request.topUpId().toString(),
                        Map.of("topUpId", request.topUpId().toString())
                )
        );

        AbacatePayCreateBillingResponse response;
        try {
            response = restClient.post()
                    .uri(CREATE_BILLING_URI)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(billingRequest)
                    .retrieve()
                    .body(AbacatePayCreateBillingResponse.class);
        } catch (HttpClientErrorException ex) {
            throw new InvalidTopUpRequestException(extractErrorMessage(ex.getResponseBodyAsString()), ex);
        }

        if (response == null || response.data() == null) {
            throw new IllegalStateException("AbacatePay did not return a response");
        }

        AbacatePayCreateBillingResponse.Data data = response.data();
        return new WalletTopUpGatewayResponse(
                data.id(),
                null,
                data.status(),
                request.amount(),
                data.brCode(),
                data.brCodeBase64(),
                null
        );
    }

    private static String extractErrorMessage(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "Pagamento recusado pelo gateway de pagamento.";
        }
        try {
            JsonNode root = OBJECT_MAPPER.readTree(responseBody);
            JsonNode error = root.path("error");
            if (!error.isMissingNode() && !error.isNull()) {
                if (error.isTextual()) {
                    String message = error.asText();
                    if (!message.isBlank()) {
                        return message;
                    }
                }
                String nestedMessage = error.path("message").asText(null);
                if (nestedMessage != null && !nestedMessage.isBlank()) {
                    return nestedMessage;
                }
            }
            String rootMessage = root.path("message").asText(null);
            if (rootMessage != null && !rootMessage.isBlank()) {
                return rootMessage;
            }
        } catch (Exception ignored) {
        }
        return responseBody;
    }
}
