package br.com.accenture.inventory.infrastructure.persistence.mapper;

import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.model.StockReservation;
import br.com.accenture.inventory.domain.pagination.Direction;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.Sort;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.entity.StockReservationJpaEntity;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PersistenceMapperTest {

    @Test
    void productMapsBetweenDomainAndEntity() {
        UUID id = UUID.randomUUID();
        Product product = Product.restore(id, "SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, "https://cdn.example.com/img.jpg", 3L);

        ProductJpaEntity entity = ProductPersistenceMapper.toEntity(product);
        Product domain = ProductPersistenceMapper.toDomain(entity);

        assertThat(ProductPersistenceMapper.toEntity(null)).isNull();
        assertThat(ProductPersistenceMapper.toDomain(null)).isNull();
        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getVersion()).isEqualTo(3L);
        assertThat(entity.getDescription()).isEqualTo("Sample description");
        assertThat(entity.getImageUrl()).isEqualTo("https://cdn.example.com/img.jpg");
        assertThat(domain.getSku()).isEqualTo("SKU-001");
        assertThat(domain.getDescription()).isEqualTo("Sample description");
        assertThat(domain.getStockQuantity()).isEqualTo(5);
        assertThat(domain.getImageUrl()).isEqualTo("https://cdn.example.com/img.jpg");
    }

    @Test
    void stockReservationMapsBetweenDomainAndEntity() {
        Product product = Product.restore(UUID.randomUUID(), "SKU-001", "Notebook", "Sample description", "Electronics", BigDecimal.TEN, 5, null, 0L);
        StockReservation reservation = StockReservation.restore(
                UUID.randomUUID(),
                UUID.randomUUID(),
                product,
                2,
                ReservationStatus.ACTIVE
        );

        StockReservationJpaEntity entity = StockReservationPersistenceMapper.toEntity(reservation);
        StockReservation domain = StockReservationPersistenceMapper.toDomain(entity);

        assertThat(StockReservationPersistenceMapper.toEntity(null)).isNull();
        assertThat(StockReservationPersistenceMapper.toDomain(null)).isNull();
        assertThat(entity.getProduct().getSku()).isEqualTo("SKU-001");
        assertThat(entity.getStatus()).isEqualTo(ReservationStatus.ACTIVE);
        assertThat(domain.getReservedQuantity()).isEqualTo(2);
        assertThat(domain.getProduct().getId()).isEqualTo(product.getId());
    }

    @Test
    void pageableMapperMapsDomainSorts() {
        PageRequest pageRequest = PageRequest.of(1, 25, List.of(
                new Sort("name", Direction.ASC),
                new Sort("createdAt", Direction.DESC)
        ));

        org.springframework.data.domain.Pageable pageable = PageableMapper.toPageable(pageRequest);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }
}
