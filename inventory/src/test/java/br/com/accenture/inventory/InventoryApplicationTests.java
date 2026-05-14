package br.com.accenture.inventory;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "spring.rabbitmq.listener.simple.auto-startup=false")
class InventoryApplicationTests {

	@Test
	void contextLoads() {
	}

	@Test
	void mainRunsSpringApplication() {
		String[] args = {"--spring.main.web-application-type=none"};

		try (MockedStatic<SpringApplication> springApplication = Mockito.mockStatic(SpringApplication.class)) {
			InventoryApplication.main(args);

			springApplication.verify(() -> SpringApplication.run(InventoryApplication.class, args));
		}
	}

}
