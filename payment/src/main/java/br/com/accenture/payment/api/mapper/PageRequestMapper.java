package br.com.accenture.payment.api.mapper;

import br.com.accenture.payment.domain.pagination.Direction;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.Sort;
import org.springframework.data.domain.Pageable;

public final class PageRequestMapper {

    private PageRequestMapper() {
    }

    public static PageRequest toDomain(Pageable pageable) {
        var sorts = pageable.getSort().stream()
                .map(order -> new Sort(
                        order.getProperty(),
                        order.getDirection() == org.springframework.data.domain.Sort.Direction.DESC
                                ? Direction.DESC
                                : Direction.ASC
                ))
                .toList();

        return new PageRequest(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                sorts
        );
    }
}