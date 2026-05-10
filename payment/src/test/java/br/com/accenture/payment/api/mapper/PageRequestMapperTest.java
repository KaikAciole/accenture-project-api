package br.com.accenture.payment.api.mapper;

import br.com.accenture.payment.domain.pagination.Direction;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestMapperTest {

    @Test
    void mapsSpringPageableToDomainPageRequest() {
        var pageable = org.springframework.data.domain.PageRequest.of(
                2,
                25,
                org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Order.desc("createdAt"),
                        org.springframework.data.domain.Sort.Order.asc("amount")
                )
        );

        var result = PageRequestMapper.toDomain(pageable);

        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(25);
        assertThat(result.sorts()).hasSize(2);
        assertThat(result.sorts().get(0).field()).isEqualTo("createdAt");
        assertThat(result.sorts().get(0).direction()).isEqualTo(Direction.DESC);
        assertThat(result.sorts().get(1).field()).isEqualTo("amount");
        assertThat(result.sorts().get(1).direction()).isEqualTo(Direction.ASC);
    }
}
