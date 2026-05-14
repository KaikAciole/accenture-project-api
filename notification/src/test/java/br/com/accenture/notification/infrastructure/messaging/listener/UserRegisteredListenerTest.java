package br.com.accenture.notification.infrastructure.messaging.listener;

import br.com.accenture.notification.application.service.WelcomeNotificationService;
import br.com.accenture.notification.infrastructure.messaging.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class UserRegisteredListenerTest {

    private final WelcomeNotificationService welcomeNotificationService = mock(WelcomeNotificationService.class);
    private final UserRegisteredListener listener = new UserRegisteredListener(welcomeNotificationService);

    @Test
    void handleDelegatesCustomerIdToWelcomeNotificationService() {
        UserRegisteredEvent event = new UserRegisteredEvent("customer-123", "user@example.com");

        listener.handle(event);

        verify(welcomeNotificationService).sendWelcome("customer-123");
        verifyNoMoreInteractions(welcomeNotificationService);
    }
}
