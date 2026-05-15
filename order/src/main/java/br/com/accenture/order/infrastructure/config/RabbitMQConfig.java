package br.com.accenture.order.infrastructure.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String PAYMENT_EXCHANGE = "payment.exchange";
    public static final String ORDER_EXCHANGE = "order.exchange";

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return new Queue("order.payment.approved.queue", true);
    }

    @Bean
    public Queue paymentRefusedQueue() {
        return new Queue("order.payment.refused.queue", true);
    }

    @Bean
    public Queue paymentCanceledQueue() {
        return new Queue("order.payment.canceled.queue", true);
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return new Queue("order.payment.refunded.queue", true);
    }

    @Bean
    public Binding bindingPaymentApproved(Queue paymentApprovedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentApprovedQueue).to(paymentExchange).with("payment.approved");
    }

    @Bean
    public Binding bindingPaymentRefused(Queue paymentRefusedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefusedQueue).to(paymentExchange).with("payment.refused");
    }

    @Bean
    public Binding bindingPaymentCanceled(Queue paymentCanceledQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCanceledQueue).to(paymentExchange).with("payment.canceled");
    }

    @Bean
    public Binding bindingPaymentRefunded(Queue paymentRefundedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefundedQueue).to(paymentExchange).with("payment.refunded");
    }
}