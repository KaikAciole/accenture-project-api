package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCanceledEvent;
import br.com.accenture.notification.infrastructure.messaging.event.OrderCreatedEvent;
import br.com.accenture.notification.infrastructure.messaging.event.OrderPaidEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class OrderNotificationService {

    static final String ORDER_CREATED_SUBJECT = "Pedido recebido - AcceStore";
    static final String ORDER_PAID_SUBJECT = "Pagamento confirmado - AcceStore";
    static final String ORDER_CANCELED_SUBJECT = "Pedido cancelado - AcceStore";

    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final NotificationRepository repository;
    private final EmailSender emailSender;
    private final CustomerLookup customerLookup;

    public OrderNotificationService(NotificationRepository repository,
                                    EmailSender emailSender,
                                    CustomerLookup customerLookup) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.customerLookup = customerLookup;
    }

    @Transactional
    public void notifyOrderCreated(OrderCreatedEvent event) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping order.created notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pedido " + event.orderId() + " recebido. Total: " + BRL.format(event.totalAmount());
        sendAndPersist(event.customerId(), email, ORDER_CREATED_SUBJECT, body,
                recipient -> emailSender.sendOrderCreatedEmail(recipient, toEmailData(event)));
    }

    @Transactional
    public void notifyOrderPaid(OrderPaidEvent event) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping order.paid notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pagamento do pedido " + event.orderId() + " confirmado.";
        sendAndPersist(event.customerId(), email, ORDER_PAID_SUBJECT, body,
                recipient -> emailSender.sendOrderPaidEmail(recipient, event.orderId()));
    }

    @Transactional
    public void notifyOrderCanceled(OrderCanceledEvent event) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping order.canceled notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pedido " + event.orderId() + " cancelado. Motivo: " + event.reason();
        sendAndPersist(event.customerId(), email, ORDER_CANCELED_SUBJECT, body,
                recipient -> emailSender.sendOrderCanceledEmail(recipient, event.orderId(), event.reason()));
    }

    private void sendAndPersist(String customerId, String recipient, String subject, String body,
                                Consumer<String> emailAction) {
        emailAction.accept(recipient);
        Notification notification = Notification.create(customerId, recipient, subject, body);
        notification.markAsSent();
        repository.save(notification);
    }

    private OrderCreatedEmailData toEmailData(OrderCreatedEvent event) {
        List<OrderCreatedEmailData.Item> items = event.items().stream()
                .map(i -> new OrderCreatedEmailData.Item(i.sku(), i.quantity()))
                .toList();
        return new OrderCreatedEmailData(event.orderId(), event.totalAmount(), items);
    }
}
