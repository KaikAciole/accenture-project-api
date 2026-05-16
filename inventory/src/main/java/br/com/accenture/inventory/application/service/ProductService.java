package br.com.accenture.inventory.application.service;

import br.com.accenture.inventory.domain.exception.DuplicateProductException;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public Product create(Product product) {
        validateSkuUniqueness(product);
        return productRepository.save(product);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> findAll(PageRequest pageRequest) {
        return productRepository.findAll(pageRequest);
    }

    @Transactional(readOnly = true)
    public PageResult<Product> findByName(String name, PageRequest pageRequest) {
        return productRepository.findByNameContainingIgnoreCase(name, pageRequest);
    }

    @Transactional(readOnly = true)
    public Product findById(UUID id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Product findBySku(String sku) {
        return productRepository.findBySku(sku)
                .orElseThrow(() -> new ProductNotFoundException(sku));
    }

    @Transactional(readOnly = true)
    public List<ProductAvailabilityResult> checkAvailability(List<ProductAvailabilityCheckItem> items) {
        List<String> skus = items.stream()
                .map(ProductAvailabilityCheckItem::sku)
                .distinct()
                .toList();

        Map<String, Product> productBySku = productRepository.findBySkuIn(skus).stream()
                .collect(Collectors.toMap(Product::getSku, Function.identity()));

        return items.stream()
                .map(item -> {
                    Product product = productBySku.get(item.sku());
                    if (product == null) {
                        return new ProductAvailabilityResult(
                                item.sku(),
                                item.quantity(),
                                0,
                                false
                        );
                    }

                    int availableQuantity = product.getStockQuantity();
                    boolean available = availableQuantity >= item.quantity();
                    return new ProductAvailabilityResult(
                            item.sku(),
                            item.quantity(),
                            availableQuantity,
                            available
                    );
                })
                .toList();
    }

    @Transactional
    public Product update(UUID id, Product updated) {
        Product existing = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        String description = (updated.getDescription() == null || updated.getDescription().isBlank())
                ? existing.getDescription()
                : updated.getDescription();

        existing.update(
                updated.getName(),
                description,
                updated.getCategory(),
                updated.getBasePrice(),
                updated.getStockQuantity(),
                updated.getImageUrl()
        );

        return productRepository.save(existing);
    }

    @Transactional
    public void delete(UUID id) {
        if (productRepository.findById(id).isEmpty()) {
            throw new ProductNotFoundException(id);
        }

        productRepository.deleteById(id);
    }

    private void validateSkuUniqueness(Product product) {
        if (productRepository.existsBySku(product.getSku())) {
            throw new DuplicateProductException("sku", product.getSku());
        }
    }

    public record ProductAvailabilityCheckItem(String sku, Integer quantity) {
    }

    public record ProductAvailabilityResult(
            String sku,
            Integer requestedQuantity,
            Integer availableQuantity,
            boolean available
    ) {
    }
}
