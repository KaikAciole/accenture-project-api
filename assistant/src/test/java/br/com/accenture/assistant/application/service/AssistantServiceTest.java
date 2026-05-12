package br.com.accenture.assistant.application.service;

import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import br.com.accenture.assistant.domain.gateway.AssistantGateway;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    @Mock
    private AssistantGateway gateway;

    @InjectMocks
    private AssistantService service;

    @Test
    void ask_shouldDelegateToGateway() {
        when(gateway.ask("Como funciona a recarga?")).thenReturn("Vai em Carteira > Recarga");

        String result = service.ask("Como funciona a recarga?");

        assertThat(result).isEqualTo("Vai em Carteira > Recarga");
        verify(gateway).ask("Como funciona a recarga?");
    }

    @Test
    void ask_shouldPropagateRateLimitException() {
        when(gateway.ask(anyString()))
                .thenThrow(new AssistantRateLimitException("limit hit", 18L, new RuntimeException()));

        assertThatThrownBy(() -> service.ask("qualquer"))
                .isInstanceOf(AssistantRateLimitException.class);
    }

    @Test
    void ask_shouldPropagateTimeoutException() {
        when(gateway.ask(anyString()))
                .thenThrow(new AssistantTimeoutException("timeout", new RuntimeException()));

        assertThatThrownBy(() -> service.ask("qualquer"))
                .isInstanceOf(AssistantTimeoutException.class);
    }

    @Test
    void ask_shouldPropagateUnavailableException() {
        when(gateway.ask(anyString()))
                .thenThrow(new AssistantUnavailableException("unavailable", new RuntimeException()));

        assertThatThrownBy(() -> service.ask("qualquer"))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void ask_shouldPropagateAuthenticationException() {
        when(gateway.ask(anyString()))
                .thenThrow(new AssistantAuthenticationException("auth failed", new RuntimeException()));

        assertThatThrownBy(() -> service.ask("qualquer"))
                .isInstanceOf(AssistantAuthenticationException.class);
    }
}
