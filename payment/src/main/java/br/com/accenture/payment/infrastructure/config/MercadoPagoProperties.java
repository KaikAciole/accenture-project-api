package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "mercado-pago")
public record MercadoPagoProperties(
        String accessToken,
        String publicKey,
        String notificationUrl
) {
}