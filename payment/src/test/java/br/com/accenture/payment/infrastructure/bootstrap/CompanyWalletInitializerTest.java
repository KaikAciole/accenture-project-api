package br.com.accenture.payment.infrastructure.bootstrap;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.infrastructure.config.PaymentWalletProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CompanyWalletInitializerTest {

    @Test
    void runDelegatesCompanyOwnerIdToWalletService() {
        UUID companyOwnerId = UUID.fromString("00000000-0000-0000-0000-000000000001");
        FakeWalletService walletService = new FakeWalletService();
        PaymentWalletProperties properties = new PaymentWalletProperties(companyOwnerId);
        CompanyWalletInitializer initializer = new CompanyWalletInitializer(walletService, properties);

        initializer.run(new DefaultApplicationArguments());

        assertThat(walletService.calls).containsExactly(companyOwnerId);
    }

    private static final class FakeWalletService extends WalletService {

        private final List<UUID> calls = new ArrayList<>();

        private FakeWalletService() {
            super(null, null);
        }

        @Override
        public void createCompanyWalletIfNotExists(UUID companyOwnerId) {
            calls.add(companyOwnerId);
        }
    }
}
