package br.com.accenture.notification.application.service;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
public class PasswordResetNotificationService {

    static final String PASSWORD_RESET_SUBJECT = "Redefinicao de senha - AcceStore";
    static final String PASSWORD_RESET_BODY = "Acesse o link enviado por e-mail para redefinir sua senha.";

    private final NotificationRepository repository;
    private final EmailSender emailSender;
    private final String resetUrlBase;

    public PasswordResetNotificationService(NotificationRepository repository,
                                            EmailSender emailSender,
                                            @Value("${frontend.password-reset-url:http://localhost:5173/reset-password}") String resetUrlBase) {
        this.repository = repository;
        this.emailSender = emailSender;
        this.resetUrlBase = resetUrlBase;
    }

    @Transactional
    public void sendPasswordReset(UUID customerId, String email, String token) {
        String separator = resetUrlBase.contains("?") ? "&" : "?";
        String resetLink = resetUrlBase + separator + "token=" + token + "&email=" + email;

        emailSender.sendPasswordResetEmail(email, resetLink);

        Notification notification = Notification.create(customerId.toString(), email, PASSWORD_RESET_SUBJECT, PASSWORD_RESET_BODY);
        notification.markAsSent();
        repository.save(notification);
    }
}
