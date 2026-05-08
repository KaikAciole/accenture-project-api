package br.com.accenture.inventory.api.mapper;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ApiMapperTest {

    @Test
    void productRequestMapsToDomainAndResponse() {
        ProductRequest request = new ProductRequest("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);

        Product product = ProductDtoMapper.toDomain(request);
        var response = ProductDtoMapper.toResponse(Product.restore(
                UUID.randomUUID(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getBasePrice(),
                product.getStockQuantity(),
                0L
        ));

        assertThat(ProductDtoMapper.toDomain(null)).isNull();
        assertThat(ProductDtoMapper.toResponse(null)).isNull();
        assertThat(product.getSku()).isEqualTo("SKU-001");
        assertThat(response.name()).isEqualTo("Notebook");
        assertThat(response.stockQuantity()).isEqualTo(5);
    }

    @Test
    void stockReservationMapsToResponse() {
        UUID productId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        Product product = Product.restore(productId, "SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5, 0L);
        StockReservation reservation = StockReservation.restore(
                reservationId,
                orderId,
                product,
                2,
                ReservationStatus.ACTIVE
        );

        var response = StockReservationDtoMapper.toResponse(reservation);

        assertThat(StockReservationDtoMapper.toResponse(null)).isNull();
        assertThat(response.id()).isEqualTo(reservationId);
        assertThat(response.orderId()).isEqualTo(orderId);
        assertThat(response.productId()).isEqualTo(productId);
        assertThat(response.productSku()).isEqualTo("SKU-001");
        assertThat(response.reservedQuantity()).isEqualTo(2);
        assertThat(response.status()).isEqualTo(ReservationStatus.ACTIVE);
    }

    @Test
    void pageableMapsToDomainPageRequest() {
        Pageable pageable = PageRequest.of(2, 15, Sort.by(
                Sort.Order.asc("name"),
                Sort.Order.desc("basePrice")
        ));

        br.com.accenture.inventory.domain.pagination.PageRequest result = PageRequestMapper.toDomain(pageable);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(15);
        assertThat(result.sorts()).hasSize(2);
        assertThat(result.sorts().get(0).field()).isEqualTo("name");
        assertThat(result.sorts().get(0).direction()).isEqualTo(br.com.accenture.inventory.domain.pagination.Direction.ASC);
        assertThat(result.sorts().get(1).field()).isEqualTo("basePrice");
        assertThat(result.sorts().get(1).direction()).isEqualTo(br.com.accenture.inventory.domain.pagination.Direction.DESC);
    }
}
