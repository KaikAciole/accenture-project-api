package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.PaymentNotificationService;
import br.com.accenture.notification.domain.enums.PaymentMethod;
import br.com.accenture.notification.infrastructure.messaging.event.PaymentCanceledEvent;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class PaymentCanceledListenerTest {

    private final PaymentNotificationService paymentNotificationService = mock(PaymentNotificationService.class);
    private final PaymentCanceledListener listener = new PaymentCanceledListener(paymentNotificationService);

    @Test
    void handleDelegatesEventToPaymentNotificationService() {
        PaymentCanceledEvent event = new PaymentCanceledEvent("payment-1", "order-1", "customer-1",
                new BigDecimal("100.00"), PaymentMethod.PIX, "Cancelado pelo cliente");

        listener.handle(event);

        verify(paymentNotificationService).notifyPaymentCanceled(event);
        verifyNoMoreInteractions(paymentNotificationService);
    }
}
