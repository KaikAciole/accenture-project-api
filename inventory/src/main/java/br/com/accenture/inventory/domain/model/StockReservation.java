package br.com.accenture.inventory.domain.model;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "stock_reservations")
public class StockReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotNull(message = "Order ID is required")
    @Column(nullable = false)
    private UUID orderId;

    @NotNull(message = "Product is required")
    @ManyToOne
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @NotNull(message = "Reserved quantity is required")
    @Min(value = 1, message = "Reserved quantity must be greater than zero")
    @Column(nullable = false)
    private Integer reservedQuantity;

    @NotNull(message = "Reservation status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReservationStatus status;
}