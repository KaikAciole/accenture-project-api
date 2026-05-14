package br.com.accenture.auth.infrastructure.messaging;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String EXCHANGE_NAME = "auth.exchange";
    public static final String QUEUE_NAME = "auth.user_registered.queue";
    public static final String ROUTING_KEY = "user.registered";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(EXCHANGE_NAME);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(QUEUE_NAME, true);
    }

    @Bean
    public Binding bindingUserRegistered(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(ROUTING_KEY);
    }
}