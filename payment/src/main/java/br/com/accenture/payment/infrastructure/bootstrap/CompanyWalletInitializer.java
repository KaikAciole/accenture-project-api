package br.com.accenture.payment.infrastructure.bootstrap;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.infrastructure.config.PaymentWalletProperties;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

@Component
public class CompanyWalletInitializer implements ApplicationRunner {

    private final WalletService walletService;
    private final PaymentWalletProperties paymentWalletProperties;

    public CompanyWalletInitializer(
            WalletService walletService,
            PaymentWalletProperties paymentWalletProperties
    ) {
        this.walletService = walletService;
        this.paymentWalletProperties = paymentWalletProperties;
    }

    @Override
    public void run(@NonNull ApplicationArguments args) {
        walletService.createCompanyWalletIfNotExists(
                paymentWalletProperties.companyOwnerId()
        );
    }
}