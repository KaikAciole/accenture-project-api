package br.com.accenture.assistant.api.controller;

import br.com.accenture.assistant.api.dto.AskRequest;
import br.com.accenture.assistant.application.service.AssistantService;
import br.com.accenture.assistant.domain.exception.AssistantAuthenticationException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import br.com.accenture.assistant.infrastructure.security.ratelimit.BucketRegistry;
import br.com.accenture.assistant.infrastructure.security.ratelimit.ConcurrencyLimiter;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import reactor.core.publisher.Flux;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AssistantController.class)
@AutoConfigureMockMvc(addFilters = false)
class AssistantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AssistantService assistantService;

    @MockitoBean
    private ConcurrencyLimiter concurrencyLimiter;

    @MockitoBean
    private BucketRegistry bucketRegistry;

    @BeforeEach
    void setUp() {
        when(concurrencyLimiter.tryAcquire()).thenReturn(true);
    }

    @Test
    void ask_shouldStreamChunksAndCompleteWithDone() throws Exception {
        when(assistantService.ask("Como funciona a recarga?"))
                .thenReturn(Flux.just("Vai em ", "Carteira > ", "Recarga."));

        String body = performStreaming(new AskRequest("Como funciona a recarga?"));

        assertThat(body).contains("event:chunk");
        assertThat(body).contains("\"content\":\"Vai em \"");
        assertThat(body).contains("\"content\":\"Carteira > \"");
        assertThat(body).contains("\"content\":\"Recarga.\"");
        assertThat(body).contains("event:done");
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
    void ask_shouldEmitErrorEventOnRateLimit() throws Exception {
        when(assistantService.ask(anyString()))
                .thenReturn(Flux.error(new AssistantRateLimitException("quota exceeded", 18L, new RuntimeException())));

        String body = performStreaming(new AskRequest("test"));

        assertThat(body).contains("event:error");
        assertThat(body).contains("\"title\":\"Rate limit exceeded\"");
        assertThat(body).contains("\"retryAfterSeconds\":18");
        assertThat(body).doesNotContain("event:done");
    }

    @Test
    void ask_shouldEmitErrorEventOnTimeout() throws Exception {
        when(assistantService.ask(anyString()))
                .thenReturn(Flux.error(new AssistantTimeoutException("timeout", new RuntimeException())));

        String body = performStreaming(new AskRequest("test"));

        assertThat(body).contains("event:error");
        assertThat(body).contains("\"title\":\"Assistant timeout\"");
    }

    @Test
    void ask_shouldEmitErrorEventOnUnavailable() throws Exception {
        when(assistantService.ask(anyString()))
                .thenReturn(Flux.error(new AssistantUnavailableException("provider down", new RuntimeException())));

        String body = performStreaming(new AskRequest("test"));

        assertThat(body).contains("event:error");
        assertThat(body).contains("\"title\":\"Assistant unavailable\"");
    }

    @Test
    void ask_shouldEmitGenericErrorEventOnAuthFailure() throws Exception {
        when(assistantService.ask(anyString()))
                .thenReturn(Flux.error(new AssistantAuthenticationException("invalid key", new RuntimeException())));

        String body = performStreaming(new AskRequest("test"));

        assertThat(body).contains("event:error");
        assertThat(body).contains("\"title\":\"Internal server error\"");
    }

    @Test
    void ask_shouldEmitErrorEventWhenConcurrencyLimitReached() throws Exception {
        when(concurrencyLimiter.tryAcquire()).thenReturn(false);

        String body = performStreaming(new AskRequest("test"));

        assertThat(body).contains("event:error");
        assertThat(body).contains("\"title\":\"Too many concurrent streams\"");
        assertThat(body).doesNotContain("event:done");
    }

    private String performStreaming(AskRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/assistant/ask")
                        .contentType(MediaType.APPLICATION_JSON)
                        .accept(MediaType.TEXT_EVENT_STREAM)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(request().asyncStarted())
                .andReturn();

        return mockMvc.perform(asyncDispatch(result))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM))
                .andReturn().getResponse().getContentAsString();
    }
}
