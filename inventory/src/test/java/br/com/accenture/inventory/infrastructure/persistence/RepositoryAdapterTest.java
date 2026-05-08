package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RepositoryAdapterTest {

    private final ProductJpaRepository productJpaRepository = mock(ProductJpaRepository.class);
    private final StockReservationJpaRepository stockReservationJpaRepository = mock(StockReservationJpaRepository.class);
    private final ProductRepositoryAdapter productAdapter = new ProductRepositoryAdapter(productJpaRepository);
    private final StockReservationRepositoryAdapter reservationAdapter = new StockReservationRepositoryAdapter(stockReservationJpaRepository);

    @Test
    void productAdapterMapsCrudOperations() {
        ProductJpaEntity entity = productEntity();
        when(productJpaRepository.save(any(ProductJpaEntity.class))).thenReturn(entity);
        when(productJpaRepository.findById(TestFixtures.PRODUCT_ID)).thenReturn(Optional.of(entity));
        when(productJpaRepository.findBySku("SKU-001")).thenReturn(Optional.of(entity));
        when(productJpaRepository.existsBySku("SKU-001")).thenReturn(true);
        when(productJpaRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(productJpaRepository.findByNameContainingIgnoreCase(eq("note"), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        assertThat(productAdapter.save(TestFixtures.restoredProduct()).getId()).isEqualTo(TestFixtures.PRODUCT_ID);
        assertThat(productAdapter.findById(TestFixtures.PRODUCT_ID)).isPresent();
        assertThat(productAdapter.findBySku("SKU-001")).isPresent();
        assertThat(productAdapter.existsBySku("SKU-001")).isTrue();
        assertThat(productAdapter.findAll(PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(productAdapter.findByNameContainingIgnoreCase("note", PageRequest.of(0, 10)).content()).hasSize(1);

        productAdapter.deleteById(TestFixtures.PRODUCT_ID);
        verify(productJpaRepository).deleteById(TestFixtures.PRODUCT_ID);
    }

    @Test
    void stockReservationAdapterMapsCrudOperations() {
        StockReservationJpaEntity entity = reservationEntity(productEntity());
        when(stockReservationJpaRepository.save(any(StockReservationJpaEntity.class))).thenReturn(entity);
        when(stockReservationJpaRepository.findById(TestFixtures.RESERVATION_ID)).thenReturn(Optional.of(entity));
        when(stockReservationJpaRepository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(stockReservationJpaRepository.findByOrderId(eq(TestFixtures.ORDER_ID), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(stockReservationJpaRepository.findByOrderIdAndStatus(eq(TestFixtures.ORDER_ID), eq(ReservationStatus.ACTIVE), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));
        when(stockReservationJpaRepository.findByProductId(eq(TestFixtures.PRODUCT_ID), any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(entity)));

        assertThat(reservationAdapter.save(TestFixtures.activeReservation()).getId()).isEqualTo(TestFixtures.RESERVATION_ID);
        assertThat(reservationAdapter.findById(TestFixtures.RESERVATION_ID)).isPresent();
        assertThat(reservationAdapter.findAll(PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(reservationAdapter.findByOrderId(TestFixtures.ORDER_ID, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(reservationAdapter.findByOrderIdAndStatus(TestFixtures.ORDER_ID, ReservationStatus.ACTIVE, PageRequest.of(0, 10)).content()).hasSize(1);
        assertThat(reservationAdapter.findByProductId(TestFixtures.PRODUCT_ID, PageRequest.of(0, 10)).content()).hasSize(1);

        reservationAdapter.deleteById(TestFixtures.RESERVATION_ID);
        verify(stockReservationJpaRepository).deleteById(TestFixtures.RESERVATION_ID);
    }

    private static ProductJpaEntity productEntity() {
        return ProductJpaEntity.builder()
                .id(TestFixtures.PRODUCT_ID)
                .sku("SKU-001")
                .name("Notebook")
                .category("Electronics")
                .basePrice(java.math.BigDecimal.TEN)
                .stockQuantity(10)
                .version(0L)
                .build();
    }

    private static StockReservationJpaEntity reservationEntity(ProductJpaEntity product) {
        return StockReservationJpaEntity.builder()
                .id(TestFixtures.RESERVATION_ID)
                .orderId(TestFixtures.ORDER_ID)
                .product(product)
                .reservedQuantity(3)
                .status(ReservationStatus.ACTIVE)
                .build();
    }
}
