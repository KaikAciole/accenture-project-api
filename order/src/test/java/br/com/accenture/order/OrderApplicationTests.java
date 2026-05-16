package br.com.accenture.order;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requer RabbitMQ disponível com filas declaradas (ex.: order.stock.failed.queue) — rodar manualmente quando broker estiver pronto")
class OrderApplicationTests {

	@Test
	void contextLoads() {
	}

}
