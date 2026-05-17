package br.com.accenture.payment.api.wallet.mapper;

import br.com.accenture.payment.api.wallet.dto.response.WalletTopUpResponse;
import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class WalletTopUpDtoMapperTest {

    @Test
    void toResponseMapsAllFields() {
        UUID id = UUID.fromString("12bce0ce-3d4b-44df-aef4-3fa3e2b86b8c");
        UUID walletId = UUID.fromString("1a345f76-23b7-4cf0-a1eb-3a93ea7d65d3");
        UUID customerId = UUID.fromString("c8fd7c95-fdcf-44c0-8b9b-5e62e1d8c2c3");
        BigDecimal amount = new BigDecimal("80.00");
        WalletTopUp topUp = WalletTopUp.restore(
                id,
                walletId,
                customerId,
                amount,
                WalletTopUpStatus.PENDING,
                null,
                null,
                Instant.parse("2026-05-09T10:00:00Z"),
                Instant.parse("2026-05-09T10:05:00Z"),
                null
        );

        WalletTopUpResponse response = WalletTopUpDtoMapper.toResponse(topUp);

        assertThat(response.id()).isEqualTo(id);
        assertThat(response.walletId()).isEqualTo(walletId);
        assertThat(response.customerId()).isEqualTo(customerId);
        assertThat(response.amount()).isEqualByComparingTo(amount);
        assertThat(response.status()).isEqualTo("PENDING");
    }
}
