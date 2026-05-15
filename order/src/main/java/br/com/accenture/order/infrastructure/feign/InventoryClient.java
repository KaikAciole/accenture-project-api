package br.com.accenture.order.infrastructure.feign;

import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityItemResponse;
import br.com.accenture.order.infrastructure.feign.dto.ProductAvailabilityRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.List;

@FeignClient(name = "inventory-service", url = "${api.inventory.url:http://localhost:8083}")
public interface InventoryClient {

    @PostMapping("/products/check-availability")
    List<ProductAvailabilityItemResponse> checkAvailability(
            @RequestBody ProductAvailabilityRequest request,
            @RequestHeader("X-Internal-Secret") String internalSecret
    );
}