package br.com.accenture.assistant.api.controller;

import br.com.accenture.assistant.api.dto.AskRequest;
import br.com.accenture.assistant.application.service.AssistantService;
import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistantService assistantService;

    @Test
    void ask_shouldReturn200OnHappyPath() throws Exception {
        when(assistantService.ask("Como funciona a recarga?"))
                .thenReturn("Vai em Carteira > Recarga.");

        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("Como funciona a recarga?"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.answer").value("Vai em Carteira > Recarga."));
    }

    @Test
    void ask_shouldReturn400OnBlankQuestion() throws Exception {
        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.question").exists());
    }

    @Test
    void ask_shouldReturn503WithRetryAfterOnRateLimit() throws Exception {
        when(assistantService.ask(anyString()))
                .thenThrow(new AssistantRateLimitException("quota exceeded", 18L, new RuntimeException()));

        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("test"))))
                .andExpect(status().isServiceUnavailable())
                .andExpect(header().string("Retry-After", "18"))
                .andExpect(jsonPath("$.title").value("Rate limit exceeded"))
                .andExpect(jsonPath("$.retryAfterSeconds").value(18));
    }

    @Test
    void ask_shouldReturn504OnTimeout() throws Exception {
        when(assistantService.ask(anyString()))
                .thenThrow(new AssistantTimeoutException("timeout", new RuntimeException()));

        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("test"))))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.title").value("Assistant timeout"));
    }

    @Test
    void ask_shouldReturn502OnUnavailable() throws Exception {
        when(assistantService.ask(anyString()))
                .thenThrow(new AssistantUnavailableException("provider down", new RuntimeException()));

        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("test"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Assistant unavailable"));
    }

    @Test
    void ask_shouldReturn500OnAuthFailure() throws Exception {
        when(assistantService.ask(anyString()))
                .thenThrow(new AssistantAuthenticationException("invalid key", new RuntimeException()));

        mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new AskRequest("test"))))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"));
    }
}
