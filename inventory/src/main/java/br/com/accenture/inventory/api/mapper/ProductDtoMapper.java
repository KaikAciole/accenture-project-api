package br.com.accenture.inventory.api.mapper;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.api.dto.response.ProductResponse;
import br.com.accenture.inventory.domain.model.Product;

public final class ProductDtoMapper {

    private ProductDtoMapper() {
    }

    public static Product toDomain(ProductRequest request) {
        if (request == null) {
            return null;
        }

        return Product.createNew(
                request.sku(),
                request.name(),
                request.category(),
                request.basePrice(),
                request.stockQuantity()
        );
    }

    public static ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getCategory(),
                product.getBasePrice(),
                product.getStockQuantity()
        );
    }
}