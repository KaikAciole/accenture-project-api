package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CepLookupRequest;
import br.com.accenture.customer.application.service.CepLookupService;
import br.com.accenture.customer.domain.exception.CepLookupException;
import br.com.accenture.customer.domain.exception.CepNotFoundException;
import br.com.accenture.customer.domain.model.CepLookupResult;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CepLookupController.class)
@AutoConfigureMockMvc(addFilters = false)
class CepLookupControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CepLookupService cepLookupService;

    @Test
    void lookup_shouldReturnAddress() throws Exception {
        when(cepLookupService.lookup("01001000")).thenReturn(new CepLookupResult(
                "01001000", "Praça da Sé", "lado ímpar", "Sé", "São Paulo", "SP"
        ));

        mockMvc.perform(post("/cep/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CepLookupRequest("01001000"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.zipCode").value("01001000"))
                .andExpect(jsonPath("$.street").value("Praça da Sé"))
                .andExpect(jsonPath("$.city").value("São Paulo"))
                .andExpect(jsonPath("$.state").value("SP"));
    }

    @Test
    void lookup_shouldReturn400OnInvalidCep() throws Exception {
        mockMvc.perform(post("/cep/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CepLookupRequest("abc"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.cep").exists());
    }

    @Test
    void lookup_shouldReturn400OnBlankCep() throws Exception {
        mockMvc.perform(post("/cep/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CepLookupRequest(""))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cep").exists());
    }

    @Test
    void lookup_shouldReturn404WhenCepNotFound() throws Exception {
        when(cepLookupService.lookup("00000000")).thenThrow(new CepNotFoundException("00000000"));

        mockMvc.perform(post("/cep/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CepLookupRequest("00000000"))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("CEP not found"));
    }

    @Test
    void lookup_shouldReturn502WhenViaCepFails() throws Exception {
        when(cepLookupService.lookup("01001000"))
                .thenThrow(new CepLookupException("Failed to reach ViaCEP", new RuntimeException()));

        mockMvc.perform(post("/cep/lookup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CepLookupRequest("01001000"))))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("CEP lookup error"));
    }

}
