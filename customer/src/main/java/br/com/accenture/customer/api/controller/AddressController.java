package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.AddressResponse;
import br.com.accenture.customer.api.dto.PageResponse;
import br.com.accenture.customer.api.mapper.AddressDtoMapper;
import br.com.accenture.customer.api.mapper.PageRequestMapper;
import br.com.accenture.customer.application.service.AddressService;
import br.com.accenture.customer.domain.model.Address;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController
@RequestMapping("/customers/{customerId}/addresses")
@Tag(name = "Addresses", description = "Operações de gerenciamento de endereços vinculados a um cliente")
public class AddressController {

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @Operation(
            summary = "Cria um novo endereço para o cliente",
            description = "Cria e associa um novo endereço ao cliente identificado pelo customerId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Endereço criado com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<AddressResponse> create(
            @Parameter(description = "Identificador do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId,
            @Valid @RequestBody AddressRequest request) {
        Address created = addressService.create(AddressDtoMapper.toDomain(request, customerId));
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();
        return ResponseEntity.created(location).body(AddressDtoMapper.toResponse(created));
    }

    @GetMapping
    @Operation(
            summary = "Lista endereços do cliente",
            description = "Retorna uma página de endereços associados ao cliente identificado pelo customerId."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Página de endereços retornada com sucesso"),
            @ApiResponse(responseCode = "404", description = "Cliente não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResponse<AddressResponse> list(
            @Parameter(description = "Identificador do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId,
            @Parameter(hidden = true) Pageable pageable) {
        PageRequest pageRequest = PageRequestMapper.toDomain(pageable);
        return PageResponse.from(
                addressService.findByCustomerId(customerId, pageRequest)
                        .map(AddressDtoMapper::toResponse)
        );
    }

    @GetMapping("/{addressId}")
    @Operation(
            summary = "Busca um endereço específico do cliente",
            description = "Retorna um endereço a partir do seu identificador, validando se pertence ao cliente informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço encontrado",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para esse cliente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AddressResponse findById(
            @Parameter(description = "Identificador do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId,
            @Parameter(description = "Identificador do endereço",
                    example = "1d2e8400-e29b-41d4-a716-446655441111")
            @PathVariable UUID addressId) {
        return AddressDtoMapper.toResponse(addressService.findByCustomerAndId(customerId, addressId));
    }

    @PutMapping("/{addressId}")
    @Operation(
            summary = "Atualiza um endereço do cliente",
            description = "Atualiza os dados de um endereço existente, validando que ele pertence ao cliente informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Endereço atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = AddressResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para esse cliente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public AddressResponse update(
            @Parameter(description = "Identificador do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId,
            @Parameter(description = "Identificador do endereço",
                    example = "1d2e8400-e29b-41d4-a716-446655441111")
            @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest request) {
        Address updated = addressService.update(
                customerId,
                addressId,
                AddressDtoMapper.toDomain(request, customerId)
        );
        return AddressDtoMapper.toResponse(updated);
    }

    @DeleteMapping("/{addressId}")
    @Operation(
            summary = "Remove um endereço do cliente",
            description = "Exclui o endereço identificado, validando que ele pertence ao cliente informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Endereço removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Endereço não encontrado para esse cliente",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador do cliente",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID customerId,
            @Parameter(description = "Identificador do endereço",
                    example = "1d2e8400-e29b-41d4-a716-446655441111")
            @PathVariable UUID addressId) {
        addressService.delete(customerId, addressId);
        return ResponseEntity.noContent().build();
    }

}
