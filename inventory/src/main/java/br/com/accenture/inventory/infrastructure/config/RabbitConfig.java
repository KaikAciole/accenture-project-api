package br.com.accenture.inventory.infrastructure.config;

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
    public TopicExchange paymentExchange(InventoryPaymentMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public TopicExchange orderExchange(InventoryOrderMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public TopicExchange stockExchange(InventoryStockMessagingProperties properties) {
        return new TopicExchange(properties.exchange());
    }

    @Bean
    public Queue paymentApprovedInventoryQueue(InventoryPaymentMessagingProperties properties) {
        return new Queue(properties.queue().approved(), true);
    }

    @Bean
    public Queue paymentRefusedInventoryQueue(InventoryPaymentMessagingProperties properties) {
        return new Queue(properties.queue().refused(), true);
    }

    @Bean
    public Queue paymentCanceledInventoryQueue(InventoryPaymentMessagingProperties properties) {
        return new Queue(properties.queue().canceled(), true);
    }

    @Bean
    public Queue orderCreatedInventoryQueue(InventoryOrderMessagingProperties properties) {
        return new Queue(properties.queue().created(), true);
    }

    @Bean
    public Queue orderCanceledInventoryQueue(InventoryOrderMessagingProperties properties) {
        return new Queue(properties.queue().canceled(), true);
    }

    @Bean
    public Binding paymentApprovedInventoryBinding(
            Queue paymentApprovedInventoryQueue,
            TopicExchange paymentExchange,
            InventoryPaymentMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(paymentApprovedInventoryQueue)
                .to(paymentExchange)
                .with(properties.routingKey().approved());
    }

    @Bean
    public Binding paymentRefusedInventoryBinding(
            Queue paymentRefusedInventoryQueue,
            TopicExchange paymentExchange,
            InventoryPaymentMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(paymentRefusedInventoryQueue)
                .to(paymentExchange)
                .with(properties.routingKey().refused());
    }

    @Bean
    public Binding paymentCanceledInventoryBinding(
            Queue paymentCanceledInventoryQueue,
            TopicExchange paymentExchange,
            InventoryPaymentMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(paymentCanceledInventoryQueue)
                .to(paymentExchange)
                .with(properties.routingKey().canceled());
    }

    @Bean
    public Binding orderCreatedInventoryBinding(
            Queue orderCreatedInventoryQueue,
            TopicExchange orderExchange,
            InventoryOrderMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(orderCreatedInventoryQueue)
                .to(orderExchange)
                .with(properties.routingKey().created());
    }

    @Bean
    public Binding orderCanceledInventoryBinding(
            Queue orderCanceledInventoryQueue,
            TopicExchange orderExchange,
            InventoryOrderMessagingProperties properties
    ) {
        return BindingBuilder
                .bind(orderCanceledInventoryQueue)
                .to(orderExchange)
                .with(properties.routingKey().canceled());
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }
}