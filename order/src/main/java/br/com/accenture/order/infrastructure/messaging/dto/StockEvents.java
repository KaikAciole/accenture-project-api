package br.com.accenture.order.infrastructure.messaging.dto;
import java.util.UUID;

public final class StockEvents {

    public record Reserved(UUID orderId) {}

    public record Failed(UUID orderId, String reason) {}

}