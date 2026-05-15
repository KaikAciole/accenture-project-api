package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.api.dto.request.ProductAvailabilityRequest;
import br.com.accenture.inventory.api.dto.response.ProductResponse;
import br.com.accenture.inventory.api.dto.response.ProductAvailabilityItemResponse;
import br.com.accenture.inventory.api.mapper.PageRequestMapper;
import br.com.accenture.inventory.api.mapper.ProductDtoMapper;
import br.com.accenture.inventory.application.service.ProductService;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageResult;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/products")
@Tag(name = "Produtos", description = "Operações para cadastro, consulta, atualização e remoção de produtos")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @Operation(
            summary = "Cadastrar um novo produto",
            description = "Cadastra um novo produto com SKU, nome, categoria, preço base e quantidade inicial em estoque."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "Produto cadastrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "Já existe um produto cadastrado com o SKU informado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<ProductResponse> create(@RequestBody @Valid ProductRequest request) {
        Product product = ProductDtoMapper.toDomain(request);
        Product created = productService.create(product);

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getId())
                .toUri();

        return ResponseEntity.created(location).body(ProductDtoMapper.toResponse(created));
    }

    @PostMapping("/check-availability")
    @Operation(
            summary = "Verificar disponibilidade de produtos em lote",
            description = "Recebe uma lista de SKUs com quantidade solicitada e retorna a disponibilidade de cada item."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Disponibilidade calculada com sucesso"),
            @ApiResponse(responseCode = "400", description = "Dados invÃ¡lidos na requisiÃ§Ã£o",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ProductAvailabilityItemResponse> checkAvailability(
            @RequestBody @Valid ProductAvailabilityRequest request) {

        return productService.checkAvailability(
                        request.items().stream()
                                .map(item -> new ProductService.ProductAvailabilityCheckItem(
                                        item.sku(),
                                        item.quantity()
                                ))
                                .toList()
                ).stream()
                .map(result -> new ProductAvailabilityItemResponse(
                        result.sku(),
                        result.requestedQuantity(),
                        result.availableQuantity(),
                        result.available()
                ))
                .toList();
    }

    @GetMapping
    @Operation(
            summary = "Listar produtos",
            description = "Retorna uma lista paginada com os produtos cadastrados."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos retornados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<ProductResponse> findAll(
            @Parameter(hidden = true) Pageable pageable) {
        return productService.findAll(PageRequestMapper.toDomain(pageable))
                .map(ProductDtoMapper::toResponse);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Buscar produtos por nome",
            description = "Retorna uma lista paginada de produtos cujo nome contenha o valor informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produtos encontrados com sucesso"),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public PageResult<ProductResponse> findByName(
            @Parameter(description = "Nome ou parte do nome do produto", example = "camisa")
            @RequestParam String name,
            @Parameter(hidden = true) Pageable pageable) {
        return productService.findByName(name, PageRequestMapper.toDomain(pageable))
                .map(ProductDtoMapper::toResponse);
    }

    @GetMapping("/sku/{sku}")
    @Operation(
            summary = "Buscar produto por SKU",
            description = "Retorna os dados de um produto a partir do SKU informado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse findBySku(
            @Parameter(description = "SKU do produto", example = "PROD-001")
            @PathVariable String sku) {
        return ProductDtoMapper.toResponse(productService.findBySku(sku));
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Buscar produto por ID",
            description = "Retorna os dados de um produto a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto encontrado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse findById(
            @Parameter(description = "Identificador do produto",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        return ProductDtoMapper.toResponse(productService.findById(id));
    }

    @PutMapping("/{id}")
    @Operation(
            summary = "Atualizar produto",
            description = "Atualiza os dados de um produto já cadastrado."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Produto atualizado com sucesso",
                    content = @Content(schema = @Schema(implementation = ProductResponse.class))),
            @ApiResponse(responseCode = "400", description = "Dados inválidos na requisição",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ProductResponse update(
            @Parameter(description = "Identificador do produto",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id,
            @RequestBody @Valid ProductRequest request) {
        Product updated = ProductDtoMapper.toDomain(request);
        Product saved = productService.update(id, updated);

        return ProductDtoMapper.toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @Operation(
            summary = "Remover produto",
            description = "Remove um produto cadastrado a partir do seu identificador."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "204", description = "Produto removido com sucesso"),
            @ApiResponse(responseCode = "404", description = "Produto não encontrado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<Void> delete(
            @Parameter(description = "Identificador do produto",
                    example = "550e8400-e29b-41d4-a716-446655440000")
            @PathVariable UUID id) {
        productService.delete(id);

        return ResponseEntity.noContent().build();
    }
}
