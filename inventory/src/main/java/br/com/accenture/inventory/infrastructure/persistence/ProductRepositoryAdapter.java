package br.com.accenture.inventory.infrastructure.persistence;

import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import br.com.accenture.inventory.infrastructure.persistence.entity.ProductJpaEntity;
import br.com.accenture.inventory.infrastructure.persistence.mapper.PageableMapper;
import br.com.accenture.inventory.infrastructure.persistence.mapper.ProductPersistenceMapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Component
public class ProductRepositoryAdapter implements ProductRepository {

    private final ProductJpaRepository jpaRepository;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public Product save(Product product) {
        var entity = ProductPersistenceMapper.toEntity(product);
        var saved = jpaRepository.save(entity);
        return ProductPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Product> findById(UUID id) {
        return jpaRepository.findById(id)
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Product> findBySku(String sku) {
        return jpaRepository.findBySku(sku)
                .map(ProductPersistenceMapper::toDomain);
    }

    @Override
    public List<Product> findBySkuIn(List<String> skus) {
        return jpaRepository.findBySkuIn(skus).stream()
                .map(ProductPersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaRepository.existsBySku(sku);
    }

    @Override
    public PageResult<Product> findByNameContainingIgnoreCase(String name, PageRequest pageRequest) {
        Page<ProductJpaEntity> page = jpaRepository.findByNameContainingIgnoreCase(
                name,
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public PageResult<Product> findAll(PageRequest pageRequest) {
        Page<ProductJpaEntity> page = jpaRepository.findAll(
                PageableMapper.toPageable(pageRequest)
        );

        return toPageResult(page);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepository.deleteById(id);
    }

    private PageResult<Product> toPageResult(Page<ProductJpaEntity> page) {
        return new PageResult<>(
                page.getContent().stream()
                        .map(ProductPersistenceMapper::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages()
        );
    }
}
