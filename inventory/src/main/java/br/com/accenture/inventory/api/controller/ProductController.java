package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.api.dto.response.ProductResponse;
import br.com.accenture.inventory.api.mapper.PageRequestMapper;
import br.com.accenture.inventory.api.mapper.ProductDtoMapper;
import br.com.accenture.inventory.application.service.ProductService;
import br.com.accenture.inventory.domain.pagination.PageResult;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductResponse create(@RequestBody @Valid ProductRequest request) {
        var product = ProductDtoMapper.toDomain(request);
        var created = productService.create(product);

        return ProductDtoMapper.toResponse(created);
    }

    @GetMapping
    public PageResult<ProductResponse> findAll(Pageable pageable) {
        return productService.findAll(PageRequestMapper.toDomain(pageable))
                .map(ProductDtoMapper::toResponse);
    }

    @GetMapping("/search")
    public PageResult<ProductResponse> findByName(@RequestParam String name, Pageable pageable) {
        return productService.findByName(name, PageRequestMapper.toDomain(pageable))
                .map(ProductDtoMapper::toResponse);
    }

    @GetMapping("/sku/{sku}")
    public ProductResponse findBySku(@PathVariable String sku) {
        return ProductDtoMapper.toResponse(productService.findBySku(sku));
    }

    @GetMapping("/{id}")
    public ProductResponse findById(@PathVariable UUID id) {
        return ProductDtoMapper.toResponse(productService.findById(id));
    }

    @PutMapping("/{id}")
    public ProductResponse update(@PathVariable UUID id,
                                  @RequestBody @Valid ProductRequest request) {
        var updated = ProductDtoMapper.toDomain(request);
        var saved = productService.update(id, updated);

        return ProductDtoMapper.toResponse(saved);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable UUID id) {
        productService.delete(id);
    }
}