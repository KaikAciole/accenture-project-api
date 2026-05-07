package br.com.accenture.customer.api.mapper;

import br.com.accenture.customer.api.dto.CepLookupResponse;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CepLookupMapperTest {

    @Test
    void toResponse_shouldMapAllFields() {
        CepLookupResult result = new CepLookupResult(
                "01001000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP"
        );

        CepLookupResponse response = CepLookupMapper.toResponse(result);

        assertThat(response.zipCode()).isEqualTo("01001000");
        assertThat(response.street()).isEqualTo("Praça da Sé");
        assertThat(response.complement()).isEqualTo("lado ímpar");
        assertThat(response.neighborhood()).isEqualTo("Sé");
        assertThat(response.city()).isEqualTo("São Paulo");
        assertThat(response.state()).isEqualTo("SP");
    }

    @Test
    void toResponse_shouldReturnNullWhenResultIsNull() {
        assertThat(CepLookupMapper.toResponse(null)).isNull();
    }

}
