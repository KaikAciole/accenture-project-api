package br.com.accenture.customer.domain.gateway;

import br.com.accenture.customer.domain.model.CepLookupResult;

public interface CepLookupGateway {

    CepLookupResult lookup(String cep);

}
