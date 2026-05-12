package br.com.accenture.payment.infrastructure.persistence.payment.mapper;

import br.com.accenture.payment.domain.pagination.Direction;
import br.com.accenture.payment.domain.pagination.PageRequest;
import org.springframework.data.domain.Pageable;

public final class PageableMapper {

    private PageableMapper() {
    }

    public static Pageable toPageable(PageRequest pageRequest) {
        var sortOrders = pageRequest.sorts().stream()
                .map(sort -> new org.springframework.data.domain.Sort.Order(
                        sort.direction() == Direction.DESC
                                ? org.springframework.data.domain.Sort.Direction.DESC
                                : org.springframework.data.domain.Sort.Direction.ASC,
                        sort.field()
                ))
                .toList();

        return org.springframework.data.domain.PageRequest.of(
                pageRequest.page(),
                pageRequest.size(),
                org.springframework.data.domain.Sort.by(sortOrders)
        );
    }
}