package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.StockReservationRequest;
import br.com.accenture.inventory.api.dto.response.StockReservationResponse;
import br.com.accenture.inventory.api.mapper.PageRequestMapper;
import br.com.accenture.inventory.api.mapper.StockReservationDtoMapper;
import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.pagination.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stock-reservations")
@Tag(name = "Reservas de estoque", description = "Operações para criação, consulta e alteração do status de reservas de estoque")
public class StockReservationController {

    private final StockReservationService stockReservationService;

    public StockReservationController(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar uma reserva de estoque",
            description = "Cria uma nova reserva de estoque para um pedido, vinculando um produto e reduzindo a quantidade disponível em estoque."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Reserva de estoque criada com sucesso",
                    content = @Content(schema = @Schema(implementation = StockReservationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Estoque insuficiente ou conflito ao atualizar o estoque",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StockReservationResponse create(@RequestBody @Valid StockReservationRequest request) {
        var reservation = stockReservationService.create(
                request.orderId(),
                request.productId(),
                request.reservedQuantity()
        );

        return StockReservationDtoMapper.toResponse(reservation);
    }

    @GetMapping
    @Operation(
            summary = "Listar reservas de estoque",
            description = "Retorna uma lista paginada com as reservas de estoque cadastradas."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas de estoque retornadas com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<StockReservationResponse> findAll(
            @Parameter(hidden = true) Pageable pageable) {
        return stockReservationService.findAll(PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar reserva de estoque por ID",
            description = "Retorna os dados de uma reserva de estoque a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de estoque encontrada com sucesso",
                    content = @Content(schema = @Schema(implementation = StockReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de estoque não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StockReservationResponse findById(
            @Parameter(description = "Identificador da reserva de estoque",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.findById(id));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "Listar reservas de estoque por pedido",
            description = "Retorna uma lista paginada com as reservas de estoque vinculadas ao pedido informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas de estoque retornadas com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<StockReservationResponse> findByOrderId(
            @Parameter(description = "Identificador do pedido",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Parameter(hidden = true) Pageable pageable) {
        return stockReservationService.findByOrderId(orderId, PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/orders/{orderId}/status/{status}")
    @Operation(
            summary = "Listar reservas de estoque por pedido e status",
            description = "Retorna uma lista paginada com as reservas de estoque vinculadas ao pedido informado e filtradas pelo status da reserva."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas de estoque retornadas com sucesso"),
            @ApiResponse(responseCode = "400", description = "Status informado inválido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<StockReservationResponse> findByOrderIdAndStatus(
            @Parameter(description = "Identificador do pedido",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId,
            @Parameter(description = "Status da reserva de estoque", example = "ACTIVE")
            @PathVariable ReservationStatus status,
            @Parameter(hidden = true) Pageable pageable) {
        return stockReservationService.findByOrderIdAndStatus(
                        orderId,
                        status,
                        PageRequestMapper.toDomain(pageable)
                )
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/products/{productId}")
    @Operation(
            summary = "Listar reservas de estoque por produto",
            description = "Retorna uma lista paginada com as reservas de estoque vinculadas ao produto informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reservas de estoque retornadas com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<StockReservationResponse> findByProductId(
            @Parameter(description = "Identificador do produto",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID productId,
            @Parameter(hidden = true) Pageable pageable) {
        return stockReservationService.findByProductId(productId, PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @PatchMapping("/{id}/confirm")
    @Operation(
            summary = "Confirmar reserva de estoque",
            description = "Confirma uma reserva de estoque ativa, alterando seu status para confirmada."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de estoque confirmada com sucesso",
                    content = @Content(schema = @Schema(implementation = StockReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de estoque não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "A reserva de estoque não está em um status permitido para confirmação",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StockReservationResponse confirm(
            @Parameter(description = "Identificador da reserva de estoque",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar reserva de estoque",
            description = "Cancela uma reserva de estoque ativa e devolve a quantidade reservada ao estoque do produto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de estoque cancelada com sucesso",
                    content = @Content(schema = @Schema(implementation = StockReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de estoque não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "A reserva de estoque não está em um status permitido para cancelamento",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StockReservationResponse cancel(
            @Parameter(description = "Identificador da reserva de estoque",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.cancel(id));
    }

    @PatchMapping("/{id}/expire")
    @Operation(
            summary = "Expirar reserva de estoque",
            description = "Expira uma reserva de estoque ativa e devolve a quantidade reservada ao estoque do produto."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Reserva de estoque expirada com sucesso",
                    content = @Content(schema = @Schema(implementation = StockReservationResponse.class))),
            @ApiResponse(responseCode = "404", description = "Reserva de estoque não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "A reserva de estoque não está em um status permitido para expiração",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public StockReservationResponse expire(
            @Parameter(description = "Identificador da reserva de estoque",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.expire(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remover reserva de estoque",
            description = "Remove uma reserva de estoque cadastrada a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Reserva de estoque removida com sucesso"),
            @ApiResponse(responseCode = "404", description = "Reserva de estoque não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(
            @Parameter(description = "Identificador da reserva de estoque",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        stockReservationService.delete(id);
    }
}