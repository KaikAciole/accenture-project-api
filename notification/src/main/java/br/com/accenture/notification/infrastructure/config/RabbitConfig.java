package br.com.accenture.notification.infrastructure.config;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.retry.RejectAndDontRequeueRecoverer;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Map;

@Configuration
public class RabbitConfig {

    public static final String AUTH_EXCHANGE = "auth.exchange";
    public static final String USER_REGISTERED_QUEUE = "auth.user_registered.queue";
    public static final String USER_REGISTERED_ROUTING_KEY = "user.registered";

    public static final String PASSWORD_RESET_REQUESTED_QUEUE = "auth.password_reset_requested.queue";
    public static final String PASSWORD_RESET_REQUESTED_ROUTING_KEY = "password.reset.requested";

    public static final String ORDER_EXCHANGE = "order.exchange";
    public static final String ORDER_CREATED_QUEUE = "notification.queue.created";
    public static final String ORDER_PAID_QUEUE = "notification.queue.paid";
    public static final String ORDER_CANCELED_QUEUE = "notification.queue.canceled";
    public static final String ORDER_CREATED_ROUTING_KEY = "order.created";
    public static final String ORDER_PAID_ROUTING_KEY = "order.paid";
    public static final String ORDER_CANCELED_ROUTING_KEY = "order.canceled";

    public static final String PAYMENT_EXCHANGE = "payment.events";
    public static final String PAYMENT_REFUSED_QUEUE = "payment.refused.notification.queue";
    public static final String PAYMENT_CANCELED_QUEUE = "payment.canceled.notification.queue";
    public static final String PAYMENT_REFUSED_ROUTING_KEY = "payment.refused";
    public static final String PAYMENT_CANCELED_ROUTING_KEY = "payment.canceled";

    public static final String STOCK_EXCHANGE = "stock.exchange";
    public static final String STOCK_RESERVATION_FAILED_QUEUE = "stock.reservation.failed.notification.queue";
    public static final String STOCK_RESERVATION_FAILED_ROUTING_KEY = "stock.reservation.failed";

    public static final String DLX_EXCHANGE = "notification.dlx";
    public static final String DLQ_QUEUE = "notification.dlq";

    private static Map<String, Object> dlxArgs() {
        return Map.of("x-dead-letter-exchange", DLX_EXCHANGE);
    }

    @Bean
    public TopicExchange authExchange() {
        return new TopicExchange(AUTH_EXCHANGE);
    }

    @Bean
    public Queue userRegisteredQueue() {
        return new Queue(USER_REGISTERED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Binding userRegisteredBinding(Queue userRegisteredQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(userRegisteredQueue).to(authExchange).with(USER_REGISTERED_ROUTING_KEY);
    }

    @Bean
    public Queue passwordResetRequestedQueue() {
        return new Queue(PASSWORD_RESET_REQUESTED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Binding passwordResetRequestedBinding(Queue passwordResetRequestedQueue, TopicExchange authExchange) {
        return BindingBuilder.bind(passwordResetRequestedQueue).to(authExchange).with(PASSWORD_RESET_REQUESTED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange orderExchange() {
        return new TopicExchange(ORDER_EXCHANGE);
    }

    @Bean
    public Queue orderCreatedQueue() {
        return new Queue(ORDER_CREATED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Queue orderPaidQueue() {
        return new Queue(ORDER_PAID_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Queue orderCanceledQueue() {
        return new Queue(ORDER_CANCELED_QUEUE, true, false, false, dlxArgs());
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
    public TopicExchange paymentExchange() {
        return new TopicExchange(PAYMENT_EXCHANGE);
    }

    @Bean
    public Queue paymentRefusedQueue() {
        return new Queue(PAYMENT_REFUSED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Queue paymentCanceledQueue() {
        return new Queue(PAYMENT_CANCELED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Binding paymentRefusedBinding(Queue paymentRefusedQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentRefusedQueue).to(paymentExchange).with(PAYMENT_REFUSED_ROUTING_KEY);
    }

    @Bean
    public Binding paymentCanceledBinding(Queue paymentCanceledQueue, TopicExchange paymentExchange) {
        return BindingBuilder.bind(paymentCanceledQueue).to(paymentExchange).with(PAYMENT_CANCELED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange stockExchange() {
        return new TopicExchange(STOCK_EXCHANGE);
    }

    @Bean
    public Queue stockReservationFailedQueue() {
        return new Queue(STOCK_RESERVATION_FAILED_QUEUE, true, false, false, dlxArgs());
    }

    @Bean
    public Binding stockReservationFailedBinding(Queue stockReservationFailedQueue, TopicExchange stockExchange) {
        return BindingBuilder.bind(stockReservationFailedQueue).to(stockExchange).with(STOCK_RESERVATION_FAILED_ROUTING_KEY);
    }

    @Bean
    public TopicExchange notificationDlx() {
        return new TopicExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue notificationDlq() {
        return new Queue(DLQ_QUEUE, true);
    }

    @Bean
    public Binding notificationDlqBinding(Queue notificationDlq, TopicExchange notificationDlx) {
        return BindingBuilder.bind(notificationDlq).to(notificationDlx).with("#");
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public MethodInterceptor retryInterceptor() {
        return RetryInterceptorBuilder.stateless()
                .maxRetries(2)
                .backOffOptions(1000, 2.0, 4000)
                .recoverer(new RejectAndDontRequeueRecoverer())
                .build();
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(ConnectionFactory connectionFactory,
                                                                               MessageConverter messageConverter,
                                                                               MethodInterceptor retryInterceptor) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(messageConverter);
        factory.setAdviceChain(retryInterceptor);
        factory.setDefaultRequeueRejected(false);
        return factory;
    }
}
