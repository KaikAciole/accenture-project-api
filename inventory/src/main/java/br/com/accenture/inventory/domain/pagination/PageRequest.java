package br.com.accenture.inventory.domain.pagination;

import br.com.accenture.inventory.domain.pagination.Sort;

import java.util.List;

public record PageRequest(int page, int size, List<Sort> sorts) {

    public PageRequest {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0) {
            throw new IllegalArgumentException("size must be > 0");
        }
        sorts = sorts == null ? List.of() : List.copyOf(sorts);
    }

    public static PageRequest of(int page, int size) {
        return new PageRequest(page, size, List.of());
    }

    public static PageRequest of(int page, int size, List<Sort> sorts) {
        return new PageRequest(page, size, sorts);
    }

}
