package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.gateway.CepLookupGateway;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.springframework.stereotype.Service;

@Service
public class CepLookupService {

    private final CepLookupGateway gateway;

    public CepLookupService(CepLookupGateway gateway) {
        this.gateway = gateway;
    }

    public CepLookupResult lookup(String cep) {
        return gateway.lookup(cep);
    }

}
