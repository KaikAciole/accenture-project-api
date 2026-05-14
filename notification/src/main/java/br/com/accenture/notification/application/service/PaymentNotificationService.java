package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentRefusedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;

@Slf4j
@Service
public class PaymentNotificationService {

    static final String PAYMENT_REFUSED_SUBJECT = "Pagamento recusado - AcceStore";
    static final String PAYMENT_CANCELED_SUBJECT = "Pagamento cancelado - AcceStore";

    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

    private final NotificationRepository repository;
    private final EmailSender emailSender;

    public PaymentNotificationService(NotificationRepository repository, EmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void notifyPaymentRefused(PaymentRefusedEvent event) {
        Optional<String> emailOpt = findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No notification record found for customerId={}, skipping payment.refused notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pagamento do pedido " + event.orderId() + " recusado. Valor: " + BRL.format(event.amount())
                + ". Motivo: " + event.failureReason();
        sendAndPersist(event.customerId(), email, PAYMENT_REFUSED_SUBJECT, body,
                recipient -> emailSender.sendPaymentRefusedEmail(recipient, event.orderId(), event.amount(),
                        event.method(), event.failureReason()));
    }

    @Transactional
    public void notifyPaymentCanceled(PaymentCanceledEvent event) {
        Optional<String> emailOpt = findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No notification record found for customerId={}, skipping payment.canceled notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pagamento do pedido " + event.orderId() + " cancelado. Valor: " + BRL.format(event.amount())
                + ". Motivo: " + event.cancellationReason();
        sendAndPersist(event.customerId(), email, PAYMENT_CANCELED_SUBJECT, body,
                recipient -> emailSender.sendPaymentCanceledEmail(recipient, event.orderId(), event.amount(),
                        event.method(), event.cancellationReason()));
    }

    private Optional<String> findEmailByCustomerId(String customerId) {
        return repository.findFirstByCustomerId(customerId).map(Notification::getRecipient);
    }

    private void sendAndPersist(String customerId, String recipient, String subject, String body,
                                Consumer<String> emailAction) {
        Notification notification = Notification.create(customerId, recipient, subject, body);
        try {
            emailAction.accept(recipient);
            notification.markAsSent();
        } catch (RuntimeException e) {
            log.error("Failed to send {} email to {}", subject, recipient, e);
            notification.markAsFailed();
        }
        repository.save(notification);
    }
}
