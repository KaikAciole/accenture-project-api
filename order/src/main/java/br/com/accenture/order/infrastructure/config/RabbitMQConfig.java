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
    public static final String STOCK_EXCHANGE = "stock.exchange";

    public static final String ORDER_DLX = "order.dlx";
    public static final String ORDER_DLQ = "order.dlq";
    public static final String ORDER_DLQ_ROUTING_KEY = "order.dead-letter";

    public static final String STOCK_RESERVED_QUEUE = "order.stock.reserved.queue";
    public static final String STOCK_FAILED_QUEUE = "order.stock.failed.queue";
    public static final String STOCK_RESERVED_ROUTING_KEY = "stock.reserved";
    public static final String STOCK_FAILED_ROUTING_KEY = "stock.reservation.failed";

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
    public DirectExchange orderDlx() {
        return new DirectExchange(ORDER_DLX);
    }

    @Bean
    public Queue orderDlq() {
        return new Queue(ORDER_DLQ, true);
    }

    @Bean
    public Binding bindingOrderDlq(Queue orderDlq, DirectExchange orderDlx) {
        return BindingBuilder.bind(orderDlq).to(orderDlx).with(ORDER_DLQ_ROUTING_KEY);
    }

    @Bean
    public Queue paymentApprovedQueue() {
        return QueueBuilder.durable("order.payment.approved.queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentRefusedQueue() {
        return QueueBuilder.durable("order.payment.refused.queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentCanceledQueue() {
        return QueueBuilder.durable("order.payment.canceled.queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentRefundedQueue() {
        return QueueBuilder.durable("order.payment.refunded.queue")
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
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

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE);
    }

    @Bean
    public Queue stockReservedQueue() {
        return QueueBuilder.durable(STOCK_RESERVED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue stockFailedQueue() {
        return QueueBuilder.durable(STOCK_FAILED_QUEUE)
                .withArgument("x-dead-letter-exchange", ORDER_DLX)
                .withArgument("x-dead-letter-routing-key", ORDER_DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Binding bindingStockReserved(Queue stockReservedQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(stockReservedQueue).to(stockExchange).with(STOCK_RESERVED_ROUTING_KEY);
    }

    @Bean
    public Binding bindingStockFailed(Queue stockFailedQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(stockFailedQueue).to(stockExchange).with(STOCK_FAILED_ROUTING_KEY);
    }
}