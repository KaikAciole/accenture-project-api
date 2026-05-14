package br.com.accenture.notification.application.port.data;

import java.math.BigDecimal;
import java.util.List;

public record OrderCreatedEmailData(
        String orderId,
        BigDecimal totalAmount,
        List<Item> items
) {
    public record Item(String sku, int quantity) {
    }
}
