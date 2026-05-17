package br.com.accenture.order.infrastructure.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMQConfigTest {

    private RabbitMQConfig config;

    @BeforeEach
    void setUp() {
        config = new RabbitMQConfig();
    }

    @Test
    @DisplayName("Deve criar MessageConverter como JacksonJsonMessageConverter")
    void shouldCreateJsonMessageConverter() {
        MessageConverter converter = config.jsonMessageConverter();

        assertThat(converter).isInstanceOf(JacksonJsonMessageConverter.class);
    }

    @Test
    @DisplayName("Deve criar os exchanges com nomes e tipos esperados")
    void shouldCreateExchangesWithExpectedNamesAndTypes() {
        TopicExchange paymentExchange = config.paymentExchange();
        TopicExchange orderExchange = config.orderExchange();
        TopicExchange stockExchange = config.stockExchange();
        DirectExchange orderDlx = config.orderDlx();

        assertThat(paymentExchange.getName()).isEqualTo("payment.events");
        assertThat(orderExchange.getName()).isEqualTo("order.exchange");
        assertThat(stockExchange.getName()).isEqualTo("stock.exchange");
        assertThat(orderDlx.getName()).isEqualTo("order.dlx");
    }

    @Test
    @DisplayName("Deve criar a DLQ duravel sem argumentos extras")
    void shouldCreateOrderDlqAsDurableQueue() {
        Queue dlq = config.orderDlq();

        assertThat(dlq.getName()).isEqualTo("order.dlq");
        assertThat(dlq.isDurable()).isTrue();
    }

    @Test
    @DisplayName("Deve criar as filas de pagamento com configuracao de dead-letter")
    void shouldCreatePaymentQueuesWithDeadLetterArguments() {
        Queue approved = config.paymentApprovedQueue();
        Queue refused = config.paymentRefusedQueue();
        Queue canceled = config.paymentCanceledQueue();
        Queue refunded = config.paymentRefundedQueue();

        assertThat(approved.getName()).isEqualTo("order.payment.approved.queue");
        assertThat(refused.getName()).isEqualTo("order.payment.refused.queue");
        assertThat(canceled.getName()).isEqualTo("order.payment.canceled.queue");
        assertThat(refunded.getName()).isEqualTo("order.payment.refunded.queue");

        for (Queue queue : new Queue[]{approved, refused, canceled, refunded}) {
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "order.dlx")
                    .containsEntry("x-dead-letter-routing-key", "order.dead-letter");
        }
    }

    @Test
    @DisplayName("Deve criar as filas de estoque com configuracao de dead-letter")
    void shouldCreateStockQueuesWithDeadLetterArguments() {
        Queue reserved = config.stockReservedQueue();
        Queue failed = config.stockFailedQueue();

        assertThat(reserved.getName()).isEqualTo("order.stock.reserved.queue");
        assertThat(failed.getName()).isEqualTo("order.stock.failed.queue");

        for (Queue queue : new Queue[]{reserved, failed}) {
            assertThat(queue.isDurable()).isTrue();
            assertThat(queue.getArguments())
                    .containsEntry("x-dead-letter-exchange", "order.dlx")
                    .containsEntry("x-dead-letter-routing-key", "order.dead-letter");
        }
    }

    @Test
    @DisplayName("Deve ligar a DLQ ao DLX com a routing key esperada")
    void shouldBindDlqToDlx() {
        Queue dlq = config.orderDlq();
        DirectExchange dlx = config.orderDlx();

        Binding binding = config.bindingOrderDlq(dlq, dlx);

        assertThat(binding.getDestination()).isEqualTo(dlq.getName());
        assertThat(binding.getExchange()).isEqualTo(dlx.getName());
        assertThat(binding.getRoutingKey()).isEqualTo("order.dead-letter");
    }

    @Test
    @DisplayName("Deve ligar cada fila de pagamento ao paymentExchange com sua routing key")
    void shouldBindPaymentQueuesToPaymentExchange() {
        TopicExchange exchange = config.paymentExchange();

        Binding approved = config.bindingPaymentApproved(config.paymentApprovedQueue(), exchange);
        Binding refused = config.bindingPaymentRefused(config.paymentRefusedQueue(), exchange);
        Binding canceled = config.bindingPaymentCanceled(config.paymentCanceledQueue(), exchange);
        Binding refunded = config.bindingPaymentRefunded(config.paymentRefundedQueue(), exchange);

        assertThat(approved.getExchange()).isEqualTo("payment.events");
        assertThat(approved.getRoutingKey()).isEqualTo("payment.approved");
        assertThat(approved.getDestination()).isEqualTo("order.payment.approved.queue");

        assertThat(refused.getRoutingKey()).isEqualTo("payment.refused");
        assertThat(refused.getDestination()).isEqualTo("order.payment.refused.queue");

        assertThat(canceled.getRoutingKey()).isEqualTo("payment.canceled");
        assertThat(canceled.getDestination()).isEqualTo("order.payment.canceled.queue");

        assertThat(refunded.getRoutingKey()).isEqualTo("payment.refunded");
        assertThat(refunded.getDestination()).isEqualTo("order.payment.refunded.queue");
    }

    @Test
    @DisplayName("Deve ligar as filas de estoque ao stockExchange com suas routing keys")
    void shouldBindStockQueuesToStockExchange() {
        TopicExchange exchange = config.stockExchange();

        Binding reserved = config.bindingStockReserved(config.stockReservedQueue(), exchange);
        Binding failed = config.bindingStockFailed(config.stockFailedQueue(), exchange);

        assertThat(reserved.getExchange()).isEqualTo("stock.exchange");
        assertThat(reserved.getRoutingKey()).isEqualTo("stock.reserved");
        assertThat(reserved.getDestination()).isEqualTo("order.stock.reserved.queue");

        assertThat(failed.getRoutingKey()).isEqualTo("stock.reservation.failed");
        assertThat(failed.getDestination()).isEqualTo("order.stock.failed.queue");
    }
}
