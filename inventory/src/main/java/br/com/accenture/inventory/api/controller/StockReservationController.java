package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.StockReservationRequest;
import br.com.accenture.inventory.api.dto.response.StockReservationResponse;
import br.com.accenture.inventory.api.mapper.PageRequestMapper;
import br.com.accenture.inventory.api.mapper.StockReservationDtoMapper;
import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.domain.enums.ReservationStatus;
import br.com.accenture.inventory.domain.pagination.PageResult;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/stock-reservations")
public class StockReservationController {

    private final StockReservationService stockReservationService;

    public StockReservationController(StockReservationService stockReservationService) {
        this.stockReservationService = stockReservationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public StockReservationResponse create(@RequestBody @Valid StockReservationRequest request) {
        var reservation = stockReservationService.create(
                request.orderId(),
                request.productId(),
                request.reservedQuantity()
        );

        return StockReservationDtoMapper.toResponse(reservation);
    }

    @GetMapping
    public PageResult<StockReservationResponse> findAll(Pageable pageable) {
        return stockReservationService.findAll(PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/{id}")
    public StockReservationResponse findById(@PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.findById(id));
    }

    @GetMapping("/orders/{orderId}")
    public PageResult<StockReservationResponse> findByOrderId(@PathVariable UUID orderId,
                                                              Pageable pageable) {
        return stockReservationService.findByOrderId(orderId, PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/orders/{orderId}/status/{status}")
    public PageResult<StockReservationResponse> findByOrderIdAndStatus(@PathVariable UUID orderId,
                                                                       @PathVariable ReservationStatus status,
                                                                       Pageable pageable) {
        return stockReservationService.findByOrderIdAndStatus(
                        orderId,
                        status,
                        PageRequestMapper.toDomain(pageable)
                )
                .map(StockReservationDtoMapper::toResponse);
    }

    @GetMapping("/products/{productId}")
    public PageResult<StockReservationResponse> findByProductId(@PathVariable UUID productId,
                                                                Pageable pageable) {
        return stockReservationService.findByProductId(productId, PageRequestMapper.toDomain(pageable))
                .map(StockReservationDtoMapper::toResponse);
    }

    @PatchMapping("/{id}/confirm")
    public StockReservationResponse confirm(@PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.confirm(id));
    }

    @PatchMapping("/{id}/cancel")
    public StockReservationResponse cancel(@PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.cancel(id));
    }

    @PatchMapping("/{id}/expire")
    public StockReservationResponse expire(@PathVariable UUID id) {
        return StockReservationDtoMapper.toResponse(stockReservationService.expire(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        stockReservationService.delete(id);
    }
}