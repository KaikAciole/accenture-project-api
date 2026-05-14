package br.com.accenture.payment.infrastructure.messaging.listener;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.infrastructure.messaging.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserRegisteredListenerTest {

    @Test
    void handleUserRegisteredDelegatesToWalletService() {
        FakeWalletService walletService = new FakeWalletService();
        UserRegisteredListener listener = new UserRegisteredListener(walletService);
        UUID customerId = UUID.fromString("2a497a58-b4e5-44ac-a79b-797ca294865e");

        listener.handleUserRegistered(new UserRegisteredEvent(customerId, "customer@example.com"));

        assertThat(walletService.createCustomerWalletIfNotExistsCalls).containsExactly(customerId);
    }

    private static final class FakeWalletService extends WalletService {

        private final List<UUID> createCustomerWalletIfNotExistsCalls = new ArrayList<>();

        private FakeWalletService() {
            super(null, null);
        }

        @Override
        public void createCustomerWalletIfNotExists(UUID customerId) {
            createCustomerWalletIfNotExistsCalls.add(customerId);
        }
    }
}
