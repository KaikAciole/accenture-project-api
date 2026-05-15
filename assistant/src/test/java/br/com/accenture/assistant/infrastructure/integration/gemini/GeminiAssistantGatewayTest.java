package br.com.accenture.assistant.infrastructure.integration.gemini;

import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import br.com.accenture.assistant.infrastructure.config.AssistantProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GeminiAssistantGatewayTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private ChatClient chatClient;

    private GeminiAssistantGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = new GeminiAssistantGateway(chatClient, new AssistantProperties(30, 60, 200));
    }

    @Test
    void ask_shouldEmitAllChunksOnHappyPath() {
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.just("Olá", ", ", "mundo"));

        List<String> chunks = gateway.ask("qualquer pergunta").collectList().block();

        assertThat(chunks).containsExactly("Olá", ", ", "mundo");
    }

    @Test
    void ask_shouldMap429ToRateLimitExceptionWithRetryAfter() {
        Throwable cause = new RuntimeException(
                "429 . You exceeded your current quota. Please retry in 17.233053101s."
        );
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Failed to generate content", cause)));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantRateLimitException.class)
                .satisfies(ex -> {
                    AssistantRateLimitException rate = (AssistantRateLimitException) ex;
                    assertThat(rate.getRetryAfterSeconds()).isEqualTo(18L);
                });
    }

    @Test
    void ask_shouldMap429WithoutRetryAfterToRateLimitExceptionWithNullRetry() {
        Throwable cause = new RuntimeException("429 . Quota exceeded");
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Failed", cause)));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantRateLimitException.class)
                .satisfies(ex -> {
                    AssistantRateLimitException rate = (AssistantRateLimitException) ex;
                    assertThat(rate.getRetryAfterSeconds()).isNull();
                });
    }

    @Test
    void ask_shouldMap401ToAuthenticationException() {
        Throwable cause = new RuntimeException("401 Unauthorized: API key not valid");
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Failed", cause)));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantAuthenticationException.class);
    }

    @Test
    void ask_shouldMap403ToAuthenticationException() {
        Throwable cause = new RuntimeException("403 Forbidden");
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Failed", cause)));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantAuthenticationException.class);
    }

    @Test
    void ask_shouldMap503ToUnavailableException() {
        Throwable cause = new RuntimeException("503 Service Unavailable");
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Failed", cause)));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void ask_shouldMapUnknownErrorToUnavailableException() {
        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.error(new RuntimeException("Connection refused")));

        assertThatThrownBy(() -> gateway.ask("test").blockLast())
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void ask_shouldTruncateStreamAtMaxChunks() {
        GeminiAssistantGateway cappedGateway =
                new GeminiAssistantGateway(chatClient, new AssistantProperties(30, 60, 3));

        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.just("a", "b", "c", "d", "e"));

        List<String> chunks = cappedGateway.ask("test").collectList().block();

        assertThat(chunks).containsExactly("a", "b", "c");
    }

    @Test
    void ask_shouldMapIdleStreamToTimeoutException() {
        GeminiAssistantGateway shortTimeoutGateway =
                new GeminiAssistantGateway(chatClient, new AssistantProperties(1, 60, 200));

        when(chatClient.prompt().user(anyString()).stream().content())
                .thenReturn(Flux.never());

        assertThatThrownBy(() -> shortTimeoutGateway.ask("test").blockLast())
                .isInstanceOf(AssistantTimeoutException.class);
    }
}
