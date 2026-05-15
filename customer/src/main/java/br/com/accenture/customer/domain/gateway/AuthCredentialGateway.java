package br.com.accenture.customer.domain.gateway;

import java.util.UUID;

public interface AuthCredentialGateway {
    void updateEmail(UUID customerId, String newEmail);
}
