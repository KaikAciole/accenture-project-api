package br.com.accenture.payment.api.payment.controller;

import br.com.accenture.payment.api.payment.dto.request.PaymentFailureRequest;
import br.com.accenture.payment.api.payment.dto.request.PaymentProcessRequest;
import br.com.accenture.payment.api.payment.dto.request.PaymentRequest;
import br.com.accenture.payment.api.payment.dto.response.PaymentResponse;
import br.com.accenture.payment.api.mapper.PageRequestMapper;
import br.com.accenture.payment.api.payment.mapper.PaymentDtoMapper;
import br.com.accenture.payment.application.service.payment.PaymentService;
import br.com.accenture.payment.domain.payment.model.Payment;
import br.com.accenture.payment.domain.pagination.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/payments")
@Tag(name = "Pagamentos", description = "Operações para criação, consulta e alteração do status de pagamentos")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(
            summary = "Criar pagamento",
            description = "Cria um novo pagamento vinculado a um pedido e a um cliente."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Pagamento criado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um pagamento para o pedido informado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse create(@RequestBody @Valid PaymentRequest request) {
        Payment created = paymentService.create(
                request.orderId(),
                request.customerId(),
                request.amount(),
                request.method()
        );

        return PaymentDtoMapper.toResponse(created);
    }

    @GetMapping
    @Operation(
            summary = "Listar pagamentos",
            description = "Retorna uma lista paginada com os pagamentos cadastrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamentos retornados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<PaymentResponse> findAll(
            @Parameter(hidden = true) Pageable pageable) {
        return paymentService.findAll(PageRequestMapper.toDomain(pageable))
                .map(PaymentDtoMapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar pagamento por ID",
            description = "Retorna os dados de um pagamento a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse findById(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return PaymentDtoMapper.toResponse(paymentService.findById(id));
    }

    @GetMapping("/orders/{orderId}")
    @Operation(
            summary = "Buscar pagamento por pedido",
            description = "Retorna os dados de um pagamento a partir do identificador do pedido."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado para o pedido informado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse findByOrderId(
            @Parameter(description = "Identificador do pedido",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID orderId) {
        return PaymentDtoMapper.toResponse(paymentService.findByOrderId(orderId));
    }

    @PatchMapping("/{id}/process")
    @Operation(
            summary = "Processar pagamento",
            description = "Altera um pagamento pendente para processamento e registra o identificador externo da transação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento enviado para processamento com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Status atual do pagamento não permite processamento",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse process(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @RequestBody @Valid PaymentProcessRequest request) {
        return PaymentDtoMapper.toResponse(
                paymentService.process(id, request.externalTransactionId())
        );
    }

    @PatchMapping("/{id}/approve")
    @Operation(
            summary = "Aprovar pagamento",
            description = "Aprova um pagamento que está em processamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento aprovado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Status atual do pagamento não permite aprovação",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse approve(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return PaymentDtoMapper.toResponse(paymentService.approve(id));
    }

    @PatchMapping("/{id}/refuse")
    @Operation(
            summary = "Recusar pagamento",
            description = "Recusa um pagamento pendente ou em processamento, registrando o motivo da recusa."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento recusado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Status atual do pagamento não permite recusa",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse refuse(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @RequestBody @Valid PaymentFailureRequest request) {
        return PaymentDtoMapper.toResponse(
                paymentService.refuse(id, request.failureReason())
        );
    }

    @PatchMapping("/{id}/cancel")
    @Operation(
            summary = "Cancelar pagamento",
            description = "Cancela um pagamento pendente ou em processamento, registrando o motivo do cancelamento."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento cancelado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Status atual do pagamento não permite cancelamento",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse cancel(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @RequestBody @Valid PaymentFailureRequest request) {
        return PaymentDtoMapper.toResponse(
                paymentService.cancel(id, request.failureReason())
        );
    }

    @PatchMapping("/{id}/refund")
    @Operation(
            summary = "Reembolsar pagamento",
            description = "Reembolsa um pagamento aprovado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Pagamento reembolsado com sucesso",
                    content = @Content(schema = @Schema(implementation = PaymentResponse.class))),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Status atual do pagamento não permite reembolso",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PaymentResponse refund(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return PaymentDtoMapper.toResponse(paymentService.refund(id));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Remover pagamento",
            description = "Remove um pagamento cadastrado a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Pagamento removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Pagamento não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public void delete(
            @Parameter(description = "Identificador do pagamento",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        paymentService.delete(id);
    }
}