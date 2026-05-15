package br.com.accenture.payment.infrastructure.messaging.listener;

import br.com.accenture.payment.application.service.payment.PaymentService;
import br.com.accenture.payment.infrastructure.messaging.event.OrderCanceledEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OrderEventListenerTest {

    @Test
    void handleOrderCanceledDelegatesToPaymentService() {
        FakePaymentService paymentService = new FakePaymentService();
        OrderEventListener listener = new OrderEventListener(paymentService);
        UUID orderId = UUID.fromString("e3bc2c53-e29c-4a19-9063-8b8cb55507d6");

        listener.handleOrderCanceled(new OrderCanceledEvent(orderId, "customer-1", "Customer canceled order"));

        assertThat(paymentService.cancelByOrderIdCalls)
                .containsExactly(new CancelByOrderIdCall(orderId, "Customer canceled order"));
    }

    private record CancelByOrderIdCall(UUID orderId, String reason) {
    }

    private static final class FakePaymentService extends PaymentService {

        private final List<CancelByOrderIdCall> cancelByOrderIdCalls = new ArrayList<>();

        private FakePaymentService() {
            super(null, null, null, null, null);
        }

        @Override
        public void cancelByOrderId(UUID orderId, String reason) {
            cancelByOrderIdCalls.add(new CancelByOrderIdCall(orderId, reason));
        }
    }
}
