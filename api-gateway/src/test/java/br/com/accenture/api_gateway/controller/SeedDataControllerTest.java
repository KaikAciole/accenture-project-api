package br.com.accenture.api_gateway.controller;

import br.com.accenture.api_gateway.dto.SeedDataResponse;
import br.com.accenture.api_gateway.service.SeedDataService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SeedDataController.class)
class SeedDataControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SeedDataService seedDataService;

    @Test
    void seedDataShouldReturnResponseFromService() throws Exception {
        SeedDataResponse response = new SeedDataResponse(
                "admin@accestore.com", true,
                3, 0,
                8, 0,
                3,
                "order-123", "PAID",
                List.of()
        );
        when(seedDataService.run()).thenReturn(response);

        mockMvc.perform(post("/api/v1/gateway/seed-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminEmail").value("admin@accestore.com"))
                .andExpect(jsonPath("$.adminCreated").value(true))
                .andExpect(jsonPath("$.customersCreated").value(3))
                .andExpect(jsonPath("$.productsCreated").value(8))
                .andExpect(jsonPath("$.walletsCredited").value(3))
                .andExpect(jsonPath("$.seedOrderId").value("order-123"))
                .andExpect(jsonPath("$.seedOrderStatus").value("PAID"))
                .andExpect(jsonPath("$.warnings").isArray());
    }

    @Test
    void seedDataShouldReturnWarningsWhenServiceReportsThem() throws Exception {
        SeedDataResponse response = new SeedDataResponse(
                "admin@accestore.com", false,
                0, 3,
                0, 8,
                0,
                null, null,
                List.of("Falha ao logar como admin", "Falha ao criar produto NB-001")
        );
        when(seedDataService.run()).thenReturn(response);

        mockMvc.perform(post("/api/v1/gateway/seed-data"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.adminCreated").value(false))
                .andExpect(jsonPath("$.customersSkipped").value(3))
                .andExpect(jsonPath("$.productsSkipped").value(8))
                .andExpect(jsonPath("$.warnings.length()").value(2));
    }
}
