package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.PasswordResetNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.PasswordResetRequestedEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PasswordResetRequestedListener {

    private final PasswordResetNotificationService service;

    public PasswordResetRequestedListener(PasswordResetNotificationService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitConfig.PASSWORD_RESET_REQUESTED_QUEUE)
    public void handle(PasswordResetRequestedEvent event) {
        log.info("Received password.reset.requested event for customerId={}", event.customerId());
        service.sendPasswordReset(event.customerId(), event.email(), event.token());
    }
}
