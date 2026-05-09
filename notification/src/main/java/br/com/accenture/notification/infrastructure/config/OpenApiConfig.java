package br.com.accenture.notification.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Notification Service API",
                version = "1.0.0",
                description = """
                        API REST do serviço de Notification responsável por receber eventos
                        de notificação via RabbitMQ, persistir o estado e disparar o envio
                        de e-mails aos destinatários.
                        """
        ),
        servers = {
                @Server(url = "http://localhost:8080", description = "Ambiente local")
        }
)
public class OpenApiConfig {
}
