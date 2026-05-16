package br.com.accenture.inventory.infrastructure.persistence.mapper;

import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;

public final class ProductPersistenceMapper {

    private ProductPersistenceMapper() {
    }

    public static ProductJpaEntity toEntity(Product product) {
        if (product == null) {
            return null;
        }

        return ProductJpaEntity.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .category(product.getCategory())
                .basePrice(product.getBasePrice())
                .stockQuantity(product.getStockQuantity())
                .imageUrl(product.getImageUrl())
                .version(product.getVersion())
                .build();
    }

    public static Product toDomain(ProductJpaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Product.restore(
                entity.getId(),
                entity.getSku(),
                entity.getName(),
                entity.getCategory(),
                entity.getBasePrice(),
                entity.getStockQuantity(),
                entity.getImageUrl(),
                entity.getVersion()
        );
    }
}