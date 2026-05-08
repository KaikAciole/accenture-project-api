package br.com.accenture.customer.api.dto;

import br.com.accenture.customer.domain.pagination.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Resposta paginada genérica")
public record PageResponse<T>(

        @Schema(description = "Itens da página corrente")
        List<T> content,

        @Schema(description = "Número da página corrente (zero-indexed)", example = "0")
        int pageNumber,

        @Schema(description = "Quantidade de itens por página", example = "20")
        int pageSize,

        @Schema(description = "Total de itens disponíveis em todas as páginas", example = "57")
        long totalElements,

        @Schema(description = "Total de páginas disponíveis", example = "3")
        int totalPages,

        @Schema(description = "Indica se existe próxima página", example = "true")
        boolean hasNext,

        @Schema(description = "Indica se existe página anterior", example = "false")
        boolean hasPrevious
) {

    public static <T> PageResponse<T> from(PageResult<T> result) {
        return new PageResponse<>(
                result.content(),
                result.pageNumber(),
                result.pageSize(),
                result.totalElements(),
                result.totalPages(),
                result.hasNext(),
                result.hasPrevious()
        );
    }

}
