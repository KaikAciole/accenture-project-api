package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.NotificationService;
import br.com.accenture.notification.infrastructure.config.RabbitConfig;
import br.com.accenture.notification.infrastructure.messaging.event.EmailNotificationEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class EmailNotificationListener {

    private final NotificationService notificationService;

    public EmailNotificationListener(NotificationService notificationService) {
        this.notificationService = notificationService;
    }

    @RabbitListener(queues = RabbitConfig.EMAIL_NOTIFICATIONS_QUEUE)
    public void handle(EmailNotificationEvent event) {
        notificationService.create(event.recipient(), event.subject(), event.body());
    }
}
