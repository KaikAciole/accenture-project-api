package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CreateCustomerInternalRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.api.dto.PageResponse;
import br.com.accenture.customer.api.dto.UpdateProfileRequest;
import br.com.accenture.customer.api.mapper.CustomerDtoMapper;
import br.com.accenture.customer.api.mapper.PageRequestMapper;
import br.com.accenture.customer.application.service.CustomerService;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@Tag(name = "Customers", description = "Operações de gerenciamento de clientes")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/internal/customers")
    @Operation(
            summary = "Cria um cliente a partir do serviço de auth (rota interna)",
            description = "Endpoint interno chamado pelo auth durante o registro. Recebe os dados " +
                    "completos do cliente (name, email, cpf, phone) coletados no formulário de registro."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Já existe cliente com o mesmo email, cpf ou phone",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CustomerResponse> createInternal(@Valid @RequestBody CreateCustomerInternalRequest request) {
        Customer created = customerService.create(CustomerDtoMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/customers/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerDtoMapper.toResponse(created));
    }

    @GetMapping("/internal/customers/{customerId}")
    @Operation(
            summary = "Busca um cliente pelo id (rota interna)",
            description = "Endpoint interno chamado por outros microsserviços (ex.: notification) " +
                    "para obter dados básicos do cliente. Protegido pelo InternalTrafficFilter."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CustomerResponse findByIdInternal(
            @Parameter(description = "Identificador único do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId) {
        Customer customer = customerService.findById(customerId);
        return CustomerDtoMapper.toResponse(customer);
    }

    @GetMapping("/customers")
    @Operation(
            summary = "Lista clientes de forma paginada",
            description = "Retorna uma página de clientes. Aceita parâmetros padrão do Pageable " +
                    "(page, size, sort)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de clientes retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<CustomerResponse> list(@Parameter(hidden = true) Pageable pageable) {
        PageRequest pageRequest = PageRequestMapper.toDomain(pageable);
        return PageResponse.from(
                customerService.findAll(pageRequest).map(CustomerDtoMapper::toResponse)
        );
    }

    @GetMapping("/customers/{id}")
    @Operation(
            summary = "Busca um cliente pelo id",
            description = "Retorna os dados de um cliente a partir do seu identificador. " +
                    "Endpoint público (autenticado via JWT no gateway)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente encontrado",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CustomerResponse findById(
            @Parameter(description = "Identificador único do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        Customer customer = customerService.findById(id);
        return CustomerDtoMapper.toResponse(customer);
    }

    @PatchMapping("/customers/{id}")
    @Operation(
            summary = "Atualiza parcialmente o perfil de um cliente",
            description = "Atualiza name, cpf e/ou phone. Campos não enviados não são modificados. " +
                    "CPF é imutável após ser definido pela primeira vez."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflito com cpf ou phone existentes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Tentativa de alterar CPF já definido",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CustomerResponse updateProfile(
            @Parameter(description = "Identificador único do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request) {
        Customer updated = customerService.update(id, CustomerDtoMapper.toDomainForUpdate(request));
        return CustomerDtoMapper.toResponse(updated);
    }

    @DeleteMapping("/customers/{id}")
    @Operation(
            summary = "Remove um cliente pelo id",
            description = "Exclui o cliente identificado. Endereços vinculados ao cliente também são removidos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Cliente removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador único do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        customerService.delete(id);
        return ResponseEntity.noContent().build();
    }

}
