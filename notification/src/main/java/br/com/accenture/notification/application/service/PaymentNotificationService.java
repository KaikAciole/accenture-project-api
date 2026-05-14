package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
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
    private final CustomerLookup customerLookup;

    public PaymentNotificationService(NotificationRepository repository,
                                      EmailSender emailSender,
                                      CustomerLookup customerLookup) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.customerLookup = customerLookup;
    }

    @Transactional
    public void notifyPaymentRefused(PaymentRefusedEvent event) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping payment.refused notification", event.customerId());
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
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(event.customerId());
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping payment.canceled notification", event.customerId());
            return;
        }
        String email = emailOpt.get();
        String body = "Pagamento do pedido " + event.orderId() + " cancelado. Valor: " + BRL.format(event.amount())
                + ". Motivo: " + event.cancellationReason();
        sendAndPersist(event.customerId(), email, PAYMENT_CANCELED_SUBJECT, body,
                recipient -> emailSender.sendPaymentCanceledEmail(recipient, event.orderId(), event.amount(),
                        event.method(), event.cancellationReason()));
    }

    private void sendAndPersist(String customerId, String recipient, String subject, String body,
                                Consumer<String> emailAction) {
        emailAction.accept(recipient);
        Notification notification = Notification.create(customerId, recipient, subject, body);
        notification.markAsSent();
        repository.save(notification);
    }
}
