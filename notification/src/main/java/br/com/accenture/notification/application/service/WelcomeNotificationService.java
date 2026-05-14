package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.CustomerLookup;
import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
public class WelcomeNotificationService {

    static final String WELCOME_SUBJECT = "Bem-vindo ao AcceStore!";
    static final String WELCOME_BODY = "Sua conta foi criada com sucesso. Bem-vindo ao AcceStore!";

    private final NotificationRepository repository;
    private final EmailSender emailSender;
    private final CustomerLookup customerLookup;

    public WelcomeNotificationService(NotificationRepository repository,
                                      EmailSender emailSender,
                                      CustomerLookup customerLookup) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.customerLookup = customerLookup;
    }

    @Transactional
    public void sendWelcome(String customerId) {
        Optional<String> emailOpt = customerLookup.findEmailByCustomerId(customerId);
        if (emailOpt.isEmpty()) {
            log.warn("No customer found for customerId={}, skipping welcome notification", customerId);
            return;
        }
        String recipient = emailOpt.get();
        emailSender.sendWelcomeEmail(recipient);
        Notification notification = Notification.create(customerId, recipient, WELCOME_SUBJECT, WELCOME_BODY);
        notification.markAsSent();
        repository.save(notification);
    }
}
