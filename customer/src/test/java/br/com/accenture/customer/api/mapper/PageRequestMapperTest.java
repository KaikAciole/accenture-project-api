package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.domain.pagination.Direction;
import br.com.accenture.customer.domain.pagination.PageRequest;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestMapperTest {

    @Test
    void toDomain_shouldMapPageNumberAndSize() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(2, 25);

        PageRequest result = PageRequestMapper.toDomain(pageable);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(25);
        assertThat(result.sorts()).isEmpty();
    }

    @Test
    void toDomain_shouldMapAscendingSort() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10, Sort.by(Sort.Direction.ASC, "name"));

        PageRequest result = PageRequestMapper.toDomain(pageable);

        assertThat(result.sorts()).hasSize(1);
        assertThat(result.sorts().get(0).field()).isEqualTo("name");
        assertThat(result.sorts().get(0).direction()).isEqualTo(Direction.ASC);
    }

    @Test
    void toDomain_shouldMapDescendingSort() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0, 10, Sort.by(Sort.Direction.DESC, "createdAt")
        );

        PageRequest result = PageRequestMapper.toDomain(pageable);

        assertThat(result.sorts()).hasSize(1);
        assertThat(result.sorts().get(0).direction()).isEqualTo(Direction.DESC);
    }

    @Test
    void toDomain_shouldMapMultipleSortOrders() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(
                0, 10,
                Sort.by(Sort.Order.asc("name"), Sort.Order.desc("createdAt"))
        );

        PageRequest result = PageRequestMapper.toDomain(pageable);

        assertThat(result.sorts()).hasSize(2);
        assertThat(result.sorts().get(0).field()).isEqualTo("name");
        assertThat(result.sorts().get(0).direction()).isEqualTo(Direction.ASC);
        assertThat(result.sorts().get(1).field()).isEqualTo("createdAt");
        assertThat(result.sorts().get(1).direction()).isEqualTo(Direction.DESC);
    }

}
