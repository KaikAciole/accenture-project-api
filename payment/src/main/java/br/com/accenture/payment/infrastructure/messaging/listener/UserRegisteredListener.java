package br.com.accenture.payment.infrastructure.messaging.listener;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.infrastructure.messaging.event.UserRegisteredEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserRegisteredListener {

    private final WalletService walletService;

    public UserRegisteredListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @RabbitListener(queues = "${payment.messaging.customer.queue.created}")
    public void handleUserRegistered(UserRegisteredEvent event) {
        walletService.createCustomerWalletIfNotExists(event.customerId());
    }
}
