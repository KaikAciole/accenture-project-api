package br.com.accenture.payment.infrastructure.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public TopicExchange paymentExchange(PaymentMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public TopicExchange orderExchange(PaymentOrderMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public TopicExchange customerExchange(PaymentCustomerMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public Queue orderCanceledPaymentQueue(PaymentOrderMessagingProperties properties) {
        return new Queue(properties.queue().canceled(), true);
    }

    @Bean
    public Queue customerCreatedPaymentQueue(PaymentCustomerMessagingProperties properties) {
        return new Queue(properties.queue().created(), true);
    }

    @Bean
    public Binding orderCanceledPaymentBinding(
            Queue orderCanceledPaymentQueue,
            TopicExchange orderExchange,
            PaymentOrderMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(orderCanceledPaymentQueue)
                .to(orderExchange)
                .with(properties.routingKey().canceled());
    }

    @Bean
    public Binding customerCreatedPaymentBinding(
            Queue customerCreatedPaymentQueue,
            TopicExchange customerExchange,
            PaymentCustomerMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(customerCreatedPaymentQueue)
                .to(customerExchange)
                .with(properties.routingKey().created());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}