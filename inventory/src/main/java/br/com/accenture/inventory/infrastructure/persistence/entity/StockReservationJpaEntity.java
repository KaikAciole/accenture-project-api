package br.com.accenture.inventory.infrastructure.persistence.entity;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Table(
        name = "stock_reservations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_stock_reservation_order_product",
                        columnNames = {"order_id", "product_id"}
                )
        }
)
public class StockReservationJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Setter(AccessLevel.NONE)
    private UUID id;

    @Column(name = "order_id", nullable = false)
    private UUID orderId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private ProductJpaEntity product;

    @Column(name = "reserved_quantity", nullable = false)
    private Integer reservedQuantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReservationStatus status;
}
