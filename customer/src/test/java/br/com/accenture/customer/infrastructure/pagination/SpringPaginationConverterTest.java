package br.com.accenture.customer.infrastructure.pagination;

import br.com.accenture.customer.domain.pagination.Direction;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import br.com.accenture.customer.domain.pagination.Sort;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SpringPaginationConverterTest {

    @Test
    void toPageable_shouldReturnUnsortedWhenSortsAreEmpty() {
        PageRequest request = PageRequest.of(2, 25);

        Pageable pageable = SpringPaginationConverter.toPageable(request);

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(25);
        assertThat(pageable.getSort().isUnsorted()).isTrue();
    }

    @Test
    void toPageable_shouldMapAscendingSort() {
        PageRequest request = PageRequest.of(0, 10, List.of(Sort.asc("name")));

        Pageable pageable = SpringPaginationConverter.toPageable(request);

        var order = pageable.getSort().getOrderFor("name");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.ASC);
    }

    @Test
    void toPageable_shouldMapDescendingSort() {
        PageRequest request = PageRequest.of(0, 10, List.of(new Sort("createdAt", Direction.DESC)));

        Pageable pageable = SpringPaginationConverter.toPageable(request);

        var order = pageable.getSort().getOrderFor("createdAt");
        assertThat(order).isNotNull();
        assertThat(order.getDirection()).isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
    }

    @Test
    void toPageResult_shouldCarryContentAndMetadata() {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(1, 2);
        Page<String> springPage = new PageImpl<>(List.of("c", "d"), pageable, 5);

        PageResult<String> result = SpringPaginationConverter.toPageResult(springPage);

        assertThat(result.content()).containsExactly("c", "d");
        assertThat(result.pageNumber()).isEqualTo(1);
        assertThat(result.pageSize()).isEqualTo(2);
        assertThat(result.totalElements()).isEqualTo(5);
        assertThat(result.totalPages()).isEqualTo(3);
    }

}
