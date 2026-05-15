package br.com.accenture.assistant.api.controller;

import br.com.accenture.assistant.api.dto.AskRequest;
import br.com.accenture.assistant.api.dto.ChunkEvent;
import br.com.accenture.assistant.api.dto.ErrorEvent;
import br.com.accenture.assistant.application.service.AssistantService;
import br.com.accenture.assistant.domain.exception.AssistantConcurrencyLimitException;
import br.com.accenture.assistant.domain.exception.AssistantRateLimitException;
import br.com.accenture.assistant.domain.exception.AssistantTimeoutException;
import br.com.accenture.assistant.domain.exception.AssistantUnavailableException;
import br.com.accenture.assistant.infrastructure.security.ratelimit.ConcurrencyLimiter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/assistant")
@Tag(name = "Assistant", description = "Assistente virtual de suporte ao usuário final")
@Slf4j
public class AssistantController {

    private static final String EVENT_CHUNK = "chunk";
    private static final String EVENT_DONE = "done";
    private static final String EVENT_ERROR = "error";

    private final AssistantService assistantService;
    private final ConcurrencyLimiter concurrencyLimiter;

    public AssistantController(AssistantService assistantService, ConcurrencyLimiter concurrencyLimiter) {
        this.assistantService = assistantService;
        this.concurrencyLimiter = concurrencyLimiter;
    }

    @PostMapping(value = "/ask", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(
            summary = "Envia uma pergunta ao assistente virtual via streaming SSE",
            description = """
                    Recebe uma pergunta em linguagem natural e devolve a resposta em fluxo de eventos SSE.
                    Eventos emitidos:
                    - chunk: pedaço de texto da resposta
                    - done: fim do streaming
                    - error: falha durante o streaming
                    """
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Streaming iniciado",
                    content = @Content(mediaType = MediaType.TEXT_EVENT_STREAM_VALUE,
                            schema = @Schema(implementation = ChunkEvent.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public Flux<ServerSentEvent<Object>> ask(@Valid @RequestBody AskRequest request) {
        if (!concurrencyLimiter.tryAcquire()) {
            return Flux.just(toErrorEvent(new AssistantConcurrencyLimitException(
                    "Maximum concurrent assistant streams reached")));
        }
        return assistantService.ask(request.question())
                .map(chunk -> ServerSentEvent.<Object>builder(new ChunkEvent(chunk)).event(EVENT_CHUNK).build())
                .concatWith(Mono.just(ServerSentEvent.<Object>builder(Map.of()).event(EVENT_DONE).build()))
                .onErrorResume(ex -> Mono.just(toErrorEvent(ex)))
                .doFinally(signal -> concurrencyLimiter.release());
    }

    private ServerSentEvent<Object> toErrorEvent(Throwable ex) {
        log.warn("Assistant stream failed: {}", ex.toString(), ex);
        ErrorEvent payload = switch (ex) {
            case AssistantRateLimitException rate -> new ErrorEvent(
                    "Rate limit exceeded",
                    "Assistente temporariamente indisponível por excesso de uso. Tente novamente em alguns segundos.",
                    rate.getRetryAfterSeconds());
            case AssistantConcurrencyLimitException ignored -> new ErrorEvent(
                    "Too many concurrent streams",
                    "Muitas conversas simultâneas com o assistente. Aguarde um instante e tente novamente.",
                    null);
            case AssistantTimeoutException ignored -> new ErrorEvent(
                    "Assistant timeout",
                    "O assistente demorou muito para responder. Tente novamente.",
                    null);
            case AssistantUnavailableException ignored -> new ErrorEvent(
                    "Assistant unavailable",
                    "Falha ao se comunicar com o assistente. Tente novamente em instantes.",
                    null);
            default -> new ErrorEvent(
                    "Internal server error",
                    "An unexpected error occurred",
                    null);
        };
        return ServerSentEvent.<Object>builder(payload).event(EVENT_ERROR).build();
    }
}
