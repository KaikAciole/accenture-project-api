package br.com.accenture.notification.application.port;

import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;

public interface EmailSender {

    void sendWelcomeEmail(String recipient);

    void sendOrderCreatedEmail(String recipient, OrderCreatedEmailData data);

    void sendOrderPaidEmail(String recipient, String orderId);

    void sendOrderCanceledEmail(String recipient, String orderId, String reason);
}
