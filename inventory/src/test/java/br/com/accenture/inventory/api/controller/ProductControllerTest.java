package br.com.accenture.inventory.api.controller;

import br.com.accenture.inventory.api.dto.request.ProductRequest;
import br.com.accenture.inventory.api.dto.request.ProductAvailabilityItemRequest;
import br.com.accenture.inventory.api.dto.request.ProductAvailabilityRequest;
import br.com.accenture.inventory.application.service.ProductService;
import br.com.accenture.inventory.domain.model.Product;
import br.com.accenture.inventory.domain.pagination.PageRequest;
import br.com.accenture.inventory.domain.pagination.PageResult;
import br.com.accenture.inventory.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    private final ProductService service = mock(ProductService.class);
    private final ProductController controller = new ProductController(service);

    @Test
    void createReturnsCreatedResponseWithLocation() {
        Product saved = TestFixtures.restoredProduct();
        when(service.create(any(Product.class))).thenReturn(saved);
        ProductRequest request = new ProductRequest("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);
        MockHttpServletRequest servletRequest = new MockHttpServletRequest("POST", "/products");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        var response = controller.create(request);

        assertThat(response.getStatusCode().value()).isEqualTo(201);
        assertThat(response.getHeaders().getLocation().toString()).endsWith("/products/" + TestFixtures.PRODUCT_ID);
        assertThat(response.getBody().id()).isEqualTo(TestFixtures.PRODUCT_ID);
        verify(service).create(any(Product.class));
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void queryEndpointsMapDomainResultsToResponses() {
        Product product = TestFixtures.restoredProduct();
        PageResult<Product> page = new PageResult<>(List.of(product), 0, 20, 1, 1);
        Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 20);
        when(service.findAll(any(PageRequest.class))).thenReturn(page);
        when(service.findByName(any(String.class), any(PageRequest.class))).thenReturn(page);
        when(service.findBySku("SKU-001")).thenReturn(product);
        when(service.findById(TestFixtures.PRODUCT_ID)).thenReturn(product);

        assertThat(controller.findAll(pageable).content()).extracting("sku").containsExactly("SKU-001");
        assertThat(controller.findByName("note", pageable).content()).extracting("name").containsExactly("Notebook");
        assertThat(controller.findBySku("SKU-001").id()).isEqualTo(TestFixtures.PRODUCT_ID);
        assertThat(controller.findById(TestFixtures.PRODUCT_ID).sku()).isEqualTo("SKU-001");
    }

    @Test
    void updateAndDeleteDelegateToService() {
        Product saved = TestFixtures.restoredProduct();
        ProductRequest request = new ProductRequest("SKU-001", "Notebook", "Electronics", BigDecimal.TEN, 5);
        when(service.update(any(), any(Product.class))).thenReturn(saved);

        var updated = controller.update(TestFixtures.PRODUCT_ID, request);
        var deleted = controller.delete(TestFixtures.PRODUCT_ID);

        assertThat(updated.id()).isEqualTo(TestFixtures.PRODUCT_ID);
        assertThat(deleted.getStatusCode().value()).isEqualTo(204);
        verify(service).update(any(), any(Product.class));
        verify(service).delete(TestFixtures.PRODUCT_ID);
    }

    @Test
    void checkAvailabilityReturnsBatchAvailabilityResponse() {
        when(service.checkAvailability(any())).thenReturn(List.of(
                new ProductService.ProductAvailabilityResult("SKU-001", 2, 5, true),
                new ProductService.ProductAvailabilityResult("SKU-999", 1, 0, false)
        ));

        ProductAvailabilityRequest request = new ProductAvailabilityRequest(List.of(
                new ProductAvailabilityItemRequest("SKU-001", 2),
                new ProductAvailabilityItemRequest("SKU-999", 1)
        ));

        var response = controller.checkAvailability(request);

        assertThat(response).hasSize(2);
        assertThat(response.get(0).sku()).isEqualTo("SKU-001");
        assertThat(response.get(0).available()).isTrue();
        assertThat(response.get(1).sku()).isEqualTo("SKU-999");
        assertThat(response.get(1).available()).isFalse();
        verify(service).checkAvailability(any());
    }
}
