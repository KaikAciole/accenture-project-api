package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.StockReservationFailedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class StockNotificationService {

    static final String STOCK_RESERVATION_FAILED_SUBJECT = "Nao foi possivel reservar seu pedido - AcceStore";

    private final NotificationRepository repository;
    private final EmailSender emailSender;
    private final CustomerLookup customerLookup;

    public StockNotificationService(NotificationRepository repository,
                                    EmailSender emailSender,
                                    CustomerLookup customerLookup) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.customerLookup = customerLookup;
    }

    @Transactional
    public void notifyStockReservationFailed(StockReservationFailedEvent event) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping stock.reservation.failed notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Falha ao reservar estoque do pedido " + event.orderId()
                + ". SKU: " + event.sku() + ", quantidade: " + event.quantity()
                + ". Motivo: " + event.reason();

        emailSender.sendStockReservationFailedEmail(email, event.orderId(), event.sku(), event.quantity(), event.reason());
        Notification notification = Notification.create(event.customerId(), email, STOCK_RESERVATION_FAILED_SUBJECT, body);
        notification.markAsSent();
        repository.save(notification);
    }
}
