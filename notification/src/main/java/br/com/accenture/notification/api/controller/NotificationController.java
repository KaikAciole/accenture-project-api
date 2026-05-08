package br.com.accenture.notification.api.controller;

import br.com.accenture.notification.api.dto.NotificationResponse;
import br.com.accenture.notification.api.mapper.NotificationApiMapper;
import br.com.accenture.notification.application.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
@Tag(name = "Notifications", description = "Operações de consulta de notificações enviadas")
public class NotificationController {

    private final NotificationService service;
    private final NotificationApiMapper mapper = new NotificationApiMapper();

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca uma notificação pelo identificador",
            description = "Retorna os dados de uma notificação específica a partir do seu UUID, incluindo status atual e timestamps de criação e envio."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Notificação encontrada",
                    content = @Content(schema = @Schema(implementation = NotificationResponse.class))),
            @ApiResponse(responseCode = "400", description = "Identificador informado não é um UUID válido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Notificação não encontrada",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public NotificationResponse findById(
            @Parameter(description = "Identificador único da notificação",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return mapper.toResponse(service.findById(id));
    }
}
