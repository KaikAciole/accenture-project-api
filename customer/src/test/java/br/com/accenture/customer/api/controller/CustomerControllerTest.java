package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CustomerRequest;
import br.com.accenture.customer.application.service.CustomerService;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Customer;
import br.com.accenture.customer.domain.pagination.PageRequest;
import br.com.accenture.customer.domain.pagination.PageResult;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CustomerService customerService;

    private Customer existing;
    private UUID existingId;
    private CustomerRequest validRequest;

    @BeforeEach
    void setUp() {
        existingId = UUID.randomUUID();
        existing = Customer.restore(
                existingId, "Maria", "maria@example.com", "12345678901", "secret123", "11999998888",
                Instant.parse("2024-01-01T10:00:00Z"), Instant.parse("2024-01-02T10:00:00Z")
        );
        validRequest = new CustomerRequest(
                "Maria", "maria@example.com", "12345678901", "secret123", "11999998888"
        );
    }

    @Test
    void create_shouldReturn201WithLocationHeader() throws Exception {
        when(customerService.create(any())).thenReturn(existing);

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/customers/" + existingId)))
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.name").value("Maria"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void create_shouldReturn400OnInvalidPayload() throws Exception {
        CustomerRequest invalid = new CustomerRequest("", "not-an-email", "123", "short", "abc");

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.cpf").exists())
                .andExpect(jsonPath("$.errors.password").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void create_shouldReturn409WhenDuplicate() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateCustomerException("cpf", "12345678901"));

        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"))
                .andExpect(jsonPath("$.detail").value("Customer already exists with cpf: 12345678901"));
    }

    @Test
    void list_shouldReturnPageWithoutNameFilter() throws Exception {
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(existingId.toString()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.pageNumber").value(0));
    }

    @Test
    void list_shouldUseFindByNameWhenNameParamProvided() throws Exception {
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerService.findByName(eq("ma"), any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/customers").param("name", "ma"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value("Maria"));
    }

    @Test
    void findById_shouldReturnCustomer() throws Exception {
        when(customerService.findById(existingId)).thenReturn(existing);

        mockMvc.perform(get("/customers/{id}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void findById_shouldReturn404WhenMissing() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id)).thenThrow(new CustomerNotFoundException(id));

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void update_shouldReturnUpdatedCustomer() throws Exception {
        when(customerService.update(eq(existingId), any())).thenReturn(existing);

        mockMvc.perform(put("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId.toString()));
    }

    @Test
    void update_shouldReturn422WhenChangingCpf() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new ImmutableFieldException("cpf"));

        mockMvc.perform(put("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"));
    }

    @Test
    void update_shouldReturn404WhenMissing() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new CustomerNotFoundException(existingId));

        mockMvc.perform(put("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isNotFound());
    }

    @Test
    void delete_shouldReturn204() throws Exception {
        doNothing().when(customerService).delete(existingId);

        mockMvc.perform(delete("/customers/{id}", existingId))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void delete_shouldReturn404WhenMissing() throws Exception {
        doThrow(new CustomerNotFoundException(existingId)).when(customerService).delete(existingId);

        mockMvc.perform(delete("/customers/{id}", existingId))
                .andExpect(status().isNotFound());
    }

    @Test
    void shouldReturn500WithProblemDetailOnUnexpectedException() throws Exception {
        UUID id = UUID.randomUUID();
        when(customerService.findById(id)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/customers/{id}", id))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

}
