package br.com.accenture.customer.infrastructure.pagination;

import br.com.accenture.customer.domain.pagination.Direction;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public final class SpringPaginationConverter {

    private SpringPaginationConverter() {
    }

    public static Pageable toPageable(PageRequest pageRequest) {
        if (pageRequest.sorts().isEmpty()) {
            return org.springframework.data.domain.PageRequest.of(pageRequest.page(), pageRequest.size());
        }
        var orders = pageRequest.sorts().stream()
                .map(s -> new Sort.Order(toSpringDirection(s.direction()), s.field()))
                .toList();
        return org.springframework.data.domain.PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                Sort.by(orders)
        );
    }

    public static <T> PageResult<T> toPageResult(Page<T> page) {
        return new PageResult<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }

    private static Sort.Direction toSpringDirection(Direction direction) {
        return direction == Direction.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
    }

}
