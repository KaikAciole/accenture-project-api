package br.com.accenture.notification.application.port;

import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import br.com.accenture.notification.domain.enums.PaymentMethod;

import java.math.BigDecimal;

public interface EmailSender {

    void sendWelcomeEmail(String recipient);

    void sendOrderCreatedEmail(String recipient, OrderCreatedEmailData data);

    void sendOrderPaidEmail(String recipient, String orderId);

    void sendOrderCanceledEmail(String recipient, String orderId, String reason);

    void sendPaymentRefusedEmail(String recipient, String orderId, BigDecimal amount, PaymentMethod method, String failureReason);

    void sendPaymentCanceledEmail(String recipient, String orderId, BigDecimal amount, PaymentMethod method, String cancellationReason);

    void sendStockReservationFailedEmail(String recipient, String orderId, String sku, int quantity, String reason);
}
