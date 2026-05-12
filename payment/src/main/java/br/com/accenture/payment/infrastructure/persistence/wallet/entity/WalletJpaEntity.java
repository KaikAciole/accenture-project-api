package br.com.accenture.payment.infrastructure.persistence.wallet.entity;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_wallet_owner",
                        columnNames = {"owner_id", "owner_type"}
                )
        }
)
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalletJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "owner_id", nullable = false, updatable = false)
    private UUID ownerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "owner_type", nullable = false, updatable = false, length = 30)
    private WalletOwnerType ownerType;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal balance;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}