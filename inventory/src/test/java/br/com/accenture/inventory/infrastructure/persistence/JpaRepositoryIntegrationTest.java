package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class JpaRepositoryIntegrationTest {

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockReservationJpaRepository reservationRepository;

    @Test
    void productRepositoryFindsBySkuAndNameIgnoringCase() {
        ProductJpaEntity product = productRepository.save(ProductJpaEntity.builder()
                .sku("SKU-001")
                .name("Notebook Gamer")
                .description("Sample description")
                .category("Electronics")
                .basePrice(BigDecimal.valueOf(2500))
                .stockQuantity(8)
                .build());

        assertThat(product.getId()).isNotNull();
        assertThat(productRepository.existsBySku("SKU-001")).isTrue();
        assertThat(productRepository.findBySku("SKU-001")).isPresent();
        assertThat(productRepository.findByNameContainingIgnoreCase("gamer", PageRequest.of(0, 10)).getContent())
                .extracting(ProductJpaEntity::getName)
                .containsExactly("Notebook Gamer");
    }

    @Test
    void reservationRepositoryFindsByOrderStatusAndProduct() {
        ProductJpaEntity product = productRepository.save(ProductJpaEntity.builder()
                .sku("SKU-002")
                .name("Mouse")
                .description("Sample description")
                .category("Accessories")
                .basePrice(BigDecimal.valueOf(100))
                .stockQuantity(12)
                .build());
        UUID orderId = UUID.randomUUID();
        StockReservationJpaEntity reservation = reservationRepository.save(StockReservationJpaEntity.builder()
                .orderId(orderId)
                .product(product)
                .reservedQuantity(2)
                .status(ReservationStatus.ACTIVE)
                .build());

        assertThat(reservation.getId()).isNotNull();
        assertThat(reservationRepository.findByOrderId(orderId, PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(reservationRepository.findByOrderIdAndStatus(orderId, ReservationStatus.ACTIVE, PageRequest.of(0, 10)).getContent()).hasSize(1);
        assertThat(reservationRepository.findByProductId(product.getId(), PageRequest.of(0, 10)).getContent()).hasSize(1);
    }
}
