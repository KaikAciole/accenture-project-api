package br.com.accenture.order.infrastructure.feign.dto;

public record ProductStockResponse(
        String sku,
        Integer stockQuantity
) {}