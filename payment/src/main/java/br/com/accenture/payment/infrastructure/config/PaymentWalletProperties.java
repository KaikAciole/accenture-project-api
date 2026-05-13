package br.com.accenture.payment.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.UUID;

@ConfigurationProperties(prefix = "payment.wallet")
public record PaymentWalletProperties(
        UUID companyOwnerId
) {
}