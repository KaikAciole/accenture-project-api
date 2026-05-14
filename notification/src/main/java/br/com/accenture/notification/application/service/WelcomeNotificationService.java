package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class WelcomeNotificationService {

    static final String WELCOME_SUBJECT = "Bem-vindo ao AcceStore!";
    static final String WELCOME_BODY = "Sua conta foi criada com sucesso. Bem-vindo ao AcceStore!";

    private final NotificationRepository repository;
    private final EmailSender emailSender;

    public WelcomeNotificationService(NotificationRepository repository, EmailSender emailSender) {
        this.repository = repository;
        this.emailSender = emailSender;
    }

    @Transactional
    public void sendWelcome(String customerId, String recipient) {
        Notification notification = Notification.create(customerId, recipient, WELCOME_SUBJECT, WELCOME_BODY);
        try {
            emailSender.sendWelcomeEmail(recipient);
            notification.markAsSent();
        } catch (RuntimeException e) {
            log.error("Failed to send welcome email to {}", recipient, e);
            notification.markAsFailed();
        }
        repository.save(notification);
    }
}
