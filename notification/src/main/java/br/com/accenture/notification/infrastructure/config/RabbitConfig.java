package br.com.accenture.notification.infrastructure.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String EMAIL_NOTIFICATIONS_QUEUE = "email.notifications";

    @Bean
    public Queue emailNotificationsQueue() {
        return new Queue(EMAIL_NOTIFICATIONS_QUEUE, true);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
