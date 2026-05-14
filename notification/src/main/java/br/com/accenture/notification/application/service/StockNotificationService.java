package br.com.accenture.notification.application.service;

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

    public StockNotificationService(NotificationRepository repository, EmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void notifyStockReservationFailed(StockReservationFailedEvent event) {
        Optional<String> emailOpt = findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No notification record found for customerId={}, skipping stock.reservation.failed notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Falha ao reservar estoque do pedido " + event.orderId()
                + ". SKU: " + event.sku() + ", quantidade: " + event.quantity()
                + ". Motivo: " + event.reason();

        Notification notification = Notification.create(event.customerId(), email, STOCK_RESERVATION_FAILED_SUBJECT, body);
        try {
            emailSender.sendStockReservationFailedEmail(email, event.orderId(), event.sku(), event.quantity(), event.reason());
            notification.markAsSent();
        } catch (RuntimeException e) {
            log.error("Failed to send {} email to {}", STOCK_RESERVATION_FAILED_SUBJECT, email, e);
            notification.markAsFailed();
        }
        repository.save(notification);
    }

    private Optional<String> findEmailByCustomerId(String customerId) {
        return repository.findFirstByCustomerId(customerId).map(Notification::getRecipient);
    }
}
