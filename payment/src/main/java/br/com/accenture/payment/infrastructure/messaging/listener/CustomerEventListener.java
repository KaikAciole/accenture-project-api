package br.com.accenture.payment.infrastructure.messaging.listener;

import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.infrastructure.messaging.event.CustomerCreatedEvent;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class CustomerEventListener {

    private final WalletService walletService;

    public CustomerEventListener(WalletService walletService) {
        this.walletService = walletService;
    }

    @RabbitListener(queues = "${payment.messaging.customer.queue.created}")
    public void handleCustomerCreated(CustomerCreatedEvent event) {
        walletService.createCustomerWalletIfNotExists(event.customerId());
    }
}