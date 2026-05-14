package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.application.service.AddressService;
import br.com.accenture.customer.domain.exception.AddressNotFoundException;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.model.Address;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AddressController.class)
@AutoConfigureMockMvc(addFilters = false)
class AddressControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AddressService addressService;

    private UUID customerId;
    private UUID addressId;
    private Address existing;
    private AddressRequest validRequest;

    @BeforeEach
    void setUp() {
        customerId = UUID.randomUUID();
        addressId = UUID.randomUUID();
        existing = Address.restore(
                addressId, customerId, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000",
                Instant.parse("2024-01-01T10:00:00Z"), Instant.parse("2024-01-02T10:00:00Z")
        );
        validRequest = new AddressRequest(
                "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000"
        );
    }

    @Test
    void create_shouldReturn201WithLocationHeader() throws Exception {
        when(addressService.create(any())).thenReturn(existing);

        mockMvc.perform(post("/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location",
                        endsWith("/customers/" + customerId + "/addresses/" + addressId)))
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()))
                .andExpect(jsonPath("$.street").value("Rua A"));
    }

    @Test
    void create_shouldReturn400OnInvalidPayload() throws Exception {
        AddressRequest invalid = new AddressRequest("", "", null, "", "", "sp", "abc");

        mockMvc.perform(post("/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.street").exists())
                .andExpect(jsonPath("$.errors.number").exists())
                .andExpect(jsonPath("$.errors.neighborhood").exists())
                .andExpect(jsonPath("$.errors.city").exists())
                .andExpect(jsonPath("$.errors.state").exists())
                .andExpect(jsonPath("$.errors.zipCode").exists());
    }

    @Test
    void create_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(addressService.create(any())).thenThrow(new CustomerNotFoundException(customerId));

        mockMvc.perform(post("/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void list_shouldReturnPagedResults() throws Exception {
        PageResult<Address> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(addressService.findByCustomerId(eq(customerId), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/customers/{customerId}/addresses", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(addressId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(addressService.findByCustomerId(eq(customerId), any(PageRequest.class)))
                .thenThrow(new CustomerNotFoundException(customerId));

        mockMvc.perform(get("/customers/{customerId}/addresses", customerId))
                .andExpect(status().isNotFound());
    }

    @Test
    void findById_shouldReturnAddress() throws Exception {
        when(addressService.findByCustomerAndId(customerId, addressId)).thenReturn(existing);

        mockMvc.perform(get("/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()))
                .andExpect(jsonPath("$.customerId").value(customerId.toString()));
    }

    @Test
    void findById_shouldReturn404WhenAddressMissing() throws Exception {
        when(addressService.findByCustomerAndId(customerId, addressId))
                .thenThrow(new AddressNotFoundException(addressId));

        mockMvc.perform(get("/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Address not found"));
    }

    @Test
    void update_shouldReturnUpdatedAddress() throws Exception {
        when(addressService.update(eq(customerId), eq(addressId), any())).thenReturn(existing);

        mockMvc.perform(put("/customers/{customerId}/addresses/{addressId}", customerId, addressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(addressId.toString()));
    }

    @Test
    void update_shouldReturn404WhenAddressMissing() throws Exception {
        when(addressService.update(eq(customerId), eq(addressId), any()))
                .thenThrow(new AddressNotFoundException(addressId));

        mockMvc.perform(put("/customers/{customerId}/addresses/{addressId}", customerId, addressId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(addressService).delete(customerId, addressId);

        mockMvc.perform(delete("/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isNoContent());
    }

    @Test
    void delete_shouldReturn404WhenAddressMissing() throws Exception {
        doThrow(new AddressNotFoundException(addressId))
                .when(addressService).delete(customerId, addressId);

        mockMvc.perform(delete("/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isNotFound());
    }

}
