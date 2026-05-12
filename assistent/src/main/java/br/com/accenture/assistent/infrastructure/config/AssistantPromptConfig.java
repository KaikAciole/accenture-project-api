package br.com.accenture.assistent.infrastructure.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AssistantPromptConfig {

    public static final String SYSTEM_PROMPT = """
            Você é um atendente virtual de suporte ao cliente.
            Responda sempre em português do Brasil, com tom amigável e direto.
            Sua função é ajudar o usuário a entender e navegar pelas funcionalidades do produto.

            Regras:
            - NÃO explique endpoints técnicos, rotas de API ou detalhes de implementação.
            - NÃO invente funcionalidades. Se não souber, oriente o usuário a procurar o suporte humano.
            - Seja conciso: respostas curtas e objetivas.
            - Não use linguagem técnica desnecessária.
            """;

    @Bean
    public ChatClient assistantChatClient(ChatClient.Builder builder) {
        return builder
                .defaultSystem(SYSTEM_PROMPT)
                .build();
    }
}
