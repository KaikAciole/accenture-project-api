package br.com.accenture.auth.infrastructure.messaging;

import org.springframework.amqp.core.TopicExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "auth.exchange";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }
}