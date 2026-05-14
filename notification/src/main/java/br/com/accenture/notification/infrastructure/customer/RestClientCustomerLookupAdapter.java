package br.com.accenture.notification.infrastructure.customer;

import br.com.accenture.notification.application.port.CustomerLookup;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Optional;

@Slf4j
@Component
public class RestClientCustomerLookupAdapter implements CustomerLookup {

    private final RestClient customerRestClient;

    public RestClientCustomerLookupAdapter(RestClient customerRestClient) {
        this.customerRestClient = customerRestClient;
    }

    @Override
    public Optional<String> findEmailByCustomerId(String customerId) {
        try {
            CustomerResponse response = customerRestClient.get()
                    .uri("/internal/customers/{id}", customerId)
                    .retrieve()
                    .body(CustomerResponse.class);
            return Optional.ofNullable(response).map(CustomerResponse::email);
        } catch (HttpClientErrorException.NotFound e) {
            log.warn("Customer {} not found in customer service", customerId);
            return Optional.empty();
        } catch (RestClientException e) {
            log.error("Failed to fetch customer {} from customer service", customerId, e);
            return Optional.empty();
        }
    }
}
