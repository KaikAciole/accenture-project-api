package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.WelcomeNotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.UserRegisteredEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class UserRegisteredListener {

    private final WelcomeNotificationService welcomeNotificationService;

    public UserRegisteredListener(WelcomeNotificationService welcomeNotificationService) {
        this.welcomeNotificationService = welcomeNotificationService;
    }

    @RabbitListener(queues = RabbitConfig.USER_REGISTERED_QUEUE)
    public void handle(UserRegisteredEvent event) {
        log.info("Received user.registered event for customerId={}", event.customerId());
        welcomeNotificationService.sendWelcome(event.customerId());
    }
}
