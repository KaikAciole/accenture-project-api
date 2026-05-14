package br.com.accenture.payment;

import br.com.accenture.payment.infrastructure.config.PaymentCustomerMessagingProperties;
import br.com.accenture.payment.infrastructure.config.PaymentMessagingProperties;
import br.com.accenture.payment.infrastructure.config.PaymentOrderMessagingProperties;
import br.com.accenture.payment.infrastructure.config.PaymentWalletProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@EnableConfigurationProperties({PaymentWalletProperties.class, PaymentMessagingProperties.class, PaymentOrderMessagingProperties.class, PaymentCustomerMessagingProperties.class})
@SpringBootApplication
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
