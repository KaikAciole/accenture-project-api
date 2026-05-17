package br.com.accenture.payment;

import br.com.accenture.payment.infrastructure.config.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.retry.annotation.EnableRetry;

@EnableConfigurationProperties({PaymentWalletProperties.class, PaymentMessagingProperties.class, PaymentOrderMessagingProperties.class, PaymentCustomerMessagingProperties.class, AbacatePayProperties.class})
@SpringBootApplication
@EnableRetry
public class PaymentApplication {

	public static void main(String[] args) {
		SpringApplication.run(PaymentApplication.class, args);
	}

}
