package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.PaymentNotificationService;
import br.com.accenture.notification.domain.enums.PaymentMethod;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentRefusedEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PaymentRefusedListenerTest {

    private final PaymentNotificationService paymentNotificationService = mock(PaymentNotificationService.class);
    private final PaymentRefusedListener listener = new PaymentRefusedListener(paymentNotificationService);

    @Test
    void handleDelegatesEventToPaymentNotificationService() {
        PaymentRefusedEvent event = new PaymentRefusedEvent("payment-1", "order-1", "customer-1",
                new BigDecimal("100.00"), PaymentMethod.CREDIT_CARD, "Cartao sem limite");

        listener.handle(event);

        verify(paymentNotificationService).notifyPaymentRefused(event);
        verifyNoMoreInteractions(paymentNotificationService);
    }
}
