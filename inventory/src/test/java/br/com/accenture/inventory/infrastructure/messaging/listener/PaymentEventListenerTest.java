package br.com.accenture.inventory.infrastructure.messaging.listener;

import br.com.accenture.inventory.application.service.StockReservationService;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentApprovedEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentCanceledEvent;
import br.com.accenture.inventory.infrastructure.messaging.event.PaymentRefusedEvent;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentEventListenerTest {

    private final StockReservationService stockReservationService = mock(StockReservationService.class);
    private final PaymentEventListener listener = new PaymentEventListener(stockReservationService);

    @Test
    void handlePaymentApprovedDelegatesToStockReservationService() {
        listener.handlePaymentApproved(new PaymentApprovedEvent(
                UUID.randomUUID(),
                TestFixtures.PAYMENT_ID,
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("100.00"),
                "PIX",
                Instant.now(),
                Instant.now()
        ));

        verify(stockReservationService).confirmByOrderId(TestFixtures.ORDER_ID);
    }

    @Test
    void handlePaymentRefusedDelegatesToStockReservationService() {
        listener.handlePaymentRefused(new PaymentRefusedEvent(
                UUID.randomUUID(),
                TestFixtures.PAYMENT_ID,
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("100.00"),
                "PIX",
                "Antifraud refused",
                Instant.now()
        ));

        verify(stockReservationService).cancelByOrderId(TestFixtures.ORDER_ID);
    }

    @Test
    void handlePaymentCanceledDelegatesToStockReservationService() {
        listener.handlePaymentCanceled(new PaymentCanceledEvent(
                UUID.randomUUID(),
                TestFixtures.PAYMENT_ID,
                TestFixtures.ORDER_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("100.00"),
                "PIX",
                "Order canceled",
                Instant.now()
        ));

        verify(stockReservationService).cancelByOrderId(TestFixtures.ORDER_ID);
    }
}
