package br.com.accenture.inventory.application.service;

import br.com.accenture.inventory.domain.exception.DuplicateProductException;
import br.com.accenture.inventory.domain.exception.ProductNotFoundException;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.domain.repository.ProductRepository;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

class ProductServiceTest {

    private final ProductRepository repository = mock(ProductRepository.class);
    private final ProductService service = new ProductService(repository);

    @Test
    void createSavesWhenSkuIsUnique() {
        Product product = TestFixtures.newProduct();
        Product saved = TestFixtures.restoredProduct();
        when(repository.existsBySku("SKU-001")).thenReturn(false);
        when(repository.save(product)).thenReturn(saved);

        Product result = service.create(product);

        assertThat(result).isSameAs(saved);
        verify(repository).existsBySku("SKU-001");
        verify(repository).save(product);
    }

    @Test
    void createRejectsDuplicateSku() {
        Product product = TestFixtures.newProduct();
        when(repository.existsBySku("SKU-001")).thenReturn(true);

        assertThatExceptionOfType(DuplicateProductException.class)
                .isThrownBy(() -> service.create(product))
                .withMessage("Product already exists with sku: SKU-001");
        verify(repository).existsBySku("SKU-001");
        verifyNoMoreInteractions(repository);
    }

    @Test
    void findDelegatesQueriesAndThrowsWhenMissing() {
        UUID id = TestFixtures.PRODUCT_ID;
        Product product = TestFixtures.restoredProduct();
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<Product> page = new PageResult<>(List.of(product), 0, 10, 1, 1);
        when(repository.findById(id)).thenReturn(Optional.of(product), Optional.empty());
        when(repository.findBySku("SKU-001")).thenReturn(Optional.of(product), Optional.empty());
        when(repository.findAll(pageRequest)).thenReturn(page);
        when(repository.findByNameContainingIgnoreCase("note", pageRequest)).thenReturn(page);

        assertThat(service.findById(id)).isSameAs(product);
        assertThat(service.findBySku("SKU-001")).isSameAs(product);
        assertThat(service.findAll(pageRequest)).isSameAs(page);
        assertThat(service.findByName("note", pageRequest)).isSameAs(page);
        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.findById(id))
                .withMessage("Product not found with id: " + id);
        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.findBySku("SKU-001"))
                .withMessage("Product not found with sku: SKU-001");
    }

    @Test
    void updateChangesExistingProductAndSaves() {
        Product existing = TestFixtures.restoredProduct();
        Product updated = Product.createNew("IGNORED-SKU", "Mouse", "Accessories", BigDecimal.valueOf(99), 20);
        when(repository.findById(TestFixtures.PRODUCT_ID)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        Product result = service.update(TestFixtures.PRODUCT_ID, updated);

        assertThat(result.getSku()).isEqualTo("SKU-001");
        assertThat(result.getName()).isEqualTo("Mouse");
        assertThat(result.getCategory()).isEqualTo("Accessories");
        assertThat(result.getBasePrice()).isEqualByComparingTo(BigDecimal.valueOf(99));
        assertThat(result.getStockQuantity()).isEqualTo(20);
        verify(repository).save(existing);
    }

    @Test
    void updateThrowsWhenProductDoesNotExist() {
        Product updated = Product.createNew("IGNORED-SKU", "Mouse", "Accessories", BigDecimal.valueOf(99), 20);
        when(repository.findById(TestFixtures.PRODUCT_ID)).thenReturn(Optional.empty());

        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.update(TestFixtures.PRODUCT_ID, updated))
                .withMessage("Product not found with id: " + TestFixtures.PRODUCT_ID);
    }

    @Test
    void deleteRequiresExistingProduct() {
        UUID id = TestFixtures.PRODUCT_ID;
        when(repository.findById(id)).thenReturn(Optional.of(TestFixtures.restoredProduct()), Optional.empty());

        service.delete(id);

        verify(repository).deleteById(id);
        assertThatExceptionOfType(ProductNotFoundException.class)
                .isThrownBy(() -> service.delete(id))
                .withMessage("Product not found with id: " + id);
    }

    @Test
    void checkAvailabilityReturnsBatchStatusForAllSkus() {
        Product availableProduct = TestFixtures.restoredProduct();
        Product lowStockProduct = Product.restore(
                UUID.randomUUID(),
                "SKU-LOW",
                "Mouse",
                "Accessories",
                BigDecimal.TEN,
                1,
                0L
        );

        when(repository.findBySkuIn(anyList())).thenReturn(List.of(availableProduct, lowStockProduct));

        List<ProductService.ProductAvailabilityResult> result = service.checkAvailability(List.of(
                new ProductService.ProductAvailabilityCheckItem("SKU-001", 2),
                new ProductService.ProductAvailabilityCheckItem("SKU-LOW", 2),
                new ProductService.ProductAvailabilityCheckItem("SKU-MISS", 1)
        ));

        assertThat(result).containsExactly(
                new ProductService.ProductAvailabilityResult("SKU-001", 2, 10, true),
                new ProductService.ProductAvailabilityResult("SKU-LOW", 2, 1, false),
                new ProductService.ProductAvailabilityResult("SKU-MISS", 1, 0, false)
        );

        verify(repository).findBySkuIn(List.of("SKU-001", "SKU-LOW", "SKU-MISS"));
    }
}
