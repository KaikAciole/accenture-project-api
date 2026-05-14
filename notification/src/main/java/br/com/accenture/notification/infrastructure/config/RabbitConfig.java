package br.com.accenture.notification.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String USER_REGISTERED_QUEUE = "auth.user_registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_QUEUE = "notification.queue.created";
    public static final String ORDER_PAID_QUEUE = "notification.queue.paid";
    public static final String ORDER_CANCELED_QUEUE = "notification.queue.canceled";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String ORDER_CANCELED_ROUTING_KEY = "order.canceled";

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(USER_REGISTERED_QUEUE, true);
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true);
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, true);
    }

    @Bean
    public Queue orderCanceledQueue() {
        return new Queue(ORDER_CANCELED_QUEUE, true);
    }

    @Bean
    public Binding orderCreatedBinding(Queue orderCreatedQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCreatedQueue).to(orderExchange).with(ORDER_CREATED_ROUTING_KEY);
    }

    @Bean
    public Binding orderPaidBinding(Queue orderPaidQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderPaidQueue).to(orderExchange).with(ORDER_PAID_ROUTING_KEY);
    }

    @Bean
    public Binding orderCanceledBinding(Queue orderCanceledQueue, TopicExchange orderExchange) {
        return BindingBuilder.bind(orderCanceledQueue).to(orderExchange).with(ORDER_CANCELED_ROUTING_KEY);
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}
