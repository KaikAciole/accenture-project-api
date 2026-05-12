package br.com.accenture.payment.infrastructure.persistence.mapper;

import br.com.accenture.payment.domain.pagination.Direction;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.Sort;
import br.com.accenture.payment.infrastructure.persistence.payment.mapper.PageableMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageableMapperTest {

    @Test
    void mapsDomainPageRequestToSpringPageable() {
        PageRequest request = PageRequest.of(
                1,
                15,
                List.of(new Sort("status", Direction.ASC), new Sort("createdAt", Direction.DESC))
        );

        var pageable = PageableMapper.toPageable(request);

        assertThat(pageable.getPageNumber()).isEqualTo(1);
        assertThat(pageable.getPageSize()).isEqualTo(15);
        assertThat(pageable.getSort().getOrderFor("status").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }
}
