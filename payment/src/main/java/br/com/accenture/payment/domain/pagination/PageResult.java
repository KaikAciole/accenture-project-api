package br.com.accenture.payment.domain.pagination;

import java.util.List;
import java.util.function.Function;

public record PageResult<T>(
        List<T> content,
        int pageNumber,
        int pageSize,
        long totalElements,
        int totalPages
) {

    public PageResult {
        content = content == null ? List.of() : List.copyOf(content);
    }

    public boolean hasNext() {
        return pageNumber + 1 < totalPages;
    }

    public boolean hasPrevious() {
        return pageNumber > 0;
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        return new PageResult<>(
                content.stream().<R>map(mapper).toList(),
                pageNumber,
                pageSize,
                totalElements,
                totalPages
        );
    }
}