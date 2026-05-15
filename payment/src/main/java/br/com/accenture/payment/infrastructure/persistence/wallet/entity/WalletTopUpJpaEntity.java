package br.com.accenture.payment.infrastructure.persistence.wallet.entity;

import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "wallet_top_ups")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletTopUpJpaEntity {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID walletId;

    @Column(nullable = false)
    private UUID customerId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private WalletTopUpStatus status;

    @Column(unique = true)
    private String externalOrderId;

    @Column(length = 2000)
    private String clientToken;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    private Instant creditedAt;
}