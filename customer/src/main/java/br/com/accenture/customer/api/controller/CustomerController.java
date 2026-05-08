package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CustomerRequest;
import br.com.accenture.customer.api.dto.CustomerResponse;
import br.com.accenture.customer.api.dto.PageResponse;
import br.com.accenture.customer.api.mapper.CustomerDtoMapper;
import br.com.accenture.customer.api.mapper.PageRequestMapper;
import br.com.accenture.customer.application.service.CustomerService;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/customers")
@Tag(name = "Customers", description = "Operações de gerenciamento de clientes")
public class CustomerController {

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo cliente",
            description = "Registra um novo cliente. CPF, e-mail e telefone devem ser únicos."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Cliente criado com sucesso",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Já existe cliente com o mesmo CPF, e-mail ou telefone",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CustomerResponse> create(@Valid @RequestBody CustomerRequest request) {
        Customer created = customerService.create(CustomerDtoMapper.toDomain(request));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(CustomerDtoMapper.toResponse(created));
    }

    @GetMapping
    @Operation(
            summary = "Lista clientes paginados",
            description = "Retorna uma página de clientes. Aceita filtro opcional por nome (contains, case-insensitive)."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de clientes retornada com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<CustomerResponse> list(
            @Parameter(description = "Filtro parcial pelo nome do cliente (case-insensitive)", example = "Maria")
            @RequestParam(required = false) String name,
            @Parameter(hidden = true) Pageable pageable) {
        PageRequest pageRequest = PageRequestMapper.toDomain(pageable);
        PageResult<Customer> customers = (name == null || name.isBlank())
                ? customerService.findAll(pageRequest)
                : customerService.findByName(name, pageRequest);
        return PageResponse.from(customers.map(CustomerDtoMapper::toResponse));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Busca um cliente pelo identificador",
            description = "Retorna um cliente específico a partir do seu UUID."
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
        return CustomerDtoMapper.toResponse(customerService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualiza um cliente existente",
            description = "Atualiza nome, e-mail, senha e telefone. O CPF é imutável após a criação."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Cliente atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = CustomerResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Conflito com e-mail ou telefone existentes",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "422", description = "Tentativa de alterar CPF (campo imutável)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CustomerResponse update(
            @Parameter(description = "Identificador único do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @Valid @RequestBody CustomerRequest request) {
        Customer updated = customerService.update(id, CustomerDtoMapper.toDomain(request));
        return CustomerDtoMapper.toResponse(updated);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remove um cliente",
            description = "Exclui o cliente identificado pelo UUID."
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
