package br.com.accenture.customer.application.service;

import br.com.accenture.customer.domain.exception.CepNotFoundException;
import br.com.accenture.customer.domain.gateway.CepLookupGateway;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CepLookupServiceTest {

    @Mock
    private CepLookupGateway gateway;

    @InjectMocks
    private CepLookupService service;

    @Test
    void lookup_shouldDelegateToGateway() {
        CepLookupResult expected = new CepLookupResult(
                "01001000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP"
        );
        when(gateway.lookup("01001000")).thenReturn(expected);

        CepLookupResult result = service.lookup("01001000");

        assertThat(result).isSameAs(expected);
        verify(gateway).lookup("01001000");
    }

    @Test
    void lookup_shouldPropagateGatewayException() {
        when(gateway.lookup("00000000")).thenThrow(new CepNotFoundException("00000000"));

        assertThatThrownBy(() -> service.lookup("00000000"))
                .isInstanceOf(CepNotFoundException.class);
    }

}
