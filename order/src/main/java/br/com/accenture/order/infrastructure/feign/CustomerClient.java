package br.com.accenture.order.infrastructure.feign;

import br.com.accenture.order.infrastructure.feign.dto.CustomerAddressResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;

import java.util.UUID;

@FeignClient(name = "customer-service", url = "${api.customer.url:http://localhost:8082}")
public interface CustomerClient {

    @GetMapping("/customers/{customerId}/addresses/{addressId}")
    CustomerAddressResponse getAddress(
            @PathVariable("customerId") UUID customerId,
            @PathVariable("addressId") UUID addressId,
            @RequestHeader("X-Internal-Secret") String internalSecret
    );
}
