package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.math.BigDecimal;

@ConfigurationProperties(prefix = "abacate-pay")
public record AbacatePayProperties(
        String apiKey,
        String webhookSecret,
        BigDecimal fixedChargeAmount
) {
}
