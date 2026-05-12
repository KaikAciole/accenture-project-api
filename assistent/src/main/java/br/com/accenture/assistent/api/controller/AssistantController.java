package br.com.accenture.assistent.api.controller;

import br.com.accenture.assistent.api.dto.AskRequest;
import br.com.accenture.assistent.api.dto.AskResponse;
import br.com.accenture.assistent.application.service.AssistantService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/assistant")
@Tag(name = "Assistant", description = "Assistente virtual de suporte ao usuário final")
public class AssistantController {

    private final AssistantService assistantService;

    public AssistantController(AssistantService assistantService) {
        this.assistantService = assistantService;
    }

    @PostMapping("/ask")
    @Operation(
            summary = "Envia uma pergunta ao assistente virtual",
            description = "Recebe uma pergunta em linguagem natural e retorna a resposta gerada pelo assistente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Resposta gerada com sucesso",
                    content = @Content(schema = @Schema(implementation = AskResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AskResponse> ask(@Valid @RequestBody AskRequest request) {
        String answer = assistantService.ask(request.question());
        return ResponseEntity.ok(new AskResponse(answer));
    }
}
