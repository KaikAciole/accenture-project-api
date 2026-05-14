package br.com.accenture.payment;

import br.com.accenture.payment.infrastructure.config.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({PaymentWalletProperties.class, PaymentMessagingProperties.class, PaymentOrderMessagingProperties.class, PaymentCustomerMessagingProperties.class, MercadoPagoProperties.class})
@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
