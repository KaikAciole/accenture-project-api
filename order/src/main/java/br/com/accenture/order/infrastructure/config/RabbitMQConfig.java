package br.com.accenture.order.infrastructure.config;

import br.com.accenture.order.application.dto.event.OrderCreatedEvent;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_EXCHANGE = "order.exchange";

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public TopicExchange paymentExchange() {
        return new TopicExchange("payment.events");
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange("stock.exchange");
    }

    @Bean
    public Queue orderPaymentApprovedQueue() {
        return new Queue("order.queue.payment-approved");
    }

    @Bean
    public Queue orderPaymentFailedQueue() {
        return new Queue("order.queue.payment-failed");
    }

    @Bean
    public Queue orderStockFailedQueue() {
        return new Queue("order.queue.stock-failed");
    }

    @Bean
    public Binding bindOrderPaymentApproved() {
        return BindingBuilder.bind(orderPaymentApprovedQueue())
                .to(paymentExchange())
                .with("payment.approved");
    }

    @Bean
    public Binding bindOrderPaymentRefused() {
        return BindingBuilder.bind(orderPaymentFailedQueue())
                .to(paymentExchange())
                .with("payment.refused");
    }

    @Bean
    public Binding bindOrderPaymentCanceled() {
        return BindingBuilder.bind(orderPaymentFailedQueue())
                .to(paymentExchange())
                .with("payment.canceled");
    }

    @Bean
    public Binding bindOrderStockFailed() {
        return BindingBuilder.bind(orderStockFailedQueue())
                .to(stockExchange())
                .with("stock.reservation.failed");
    }
}