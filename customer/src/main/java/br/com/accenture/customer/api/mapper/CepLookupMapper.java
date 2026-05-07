package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.CepLookupResponse;
import br.com.accenture.customer.domain.model.CepLookupResult;

public final class CepLookupMapper {

    private CepLookupMapper() {
    }

    public static CepLookupResponse toResponse(CepLookupResult result) {
        if (result == null) {
            return null;
        }
        return new CepLookupResponse(
                result.zipCode(),
                result.street(),
                result.complement(),
                result.neighborhood(),
                result.city(),
                result.state()
        );
    }

}
