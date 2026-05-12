package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CreateCustomerInternalRequest;
import br.com.accenture.customer.api.dto.UpdateProfileRequest;
import br.com.accenture.customer.application.service.CustomerService;
import br.com.accenture.customer.domain.exception.CustomerNotFoundException;
import br.com.accenture.customer.domain.exception.DuplicateCustomerException;
import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import br.com.accenture.customer.domain.model.Customer;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @BeforeEach
    void setUp() {
        existingId = UUID.randomUUID();
        existing = Customer.restore(
                existingId, "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.parse("2024-01-01T10:00:00Z"), Instant.parse("2024-01-02T10:00:00Z")
        );
    }

    @Test
    void createInternal_shouldReturn201WithLocationHeader() throws Exception {
        Customer minimal = Customer.restore(
                existingId, null, "maria@example.com", null, null,
                Instant.parse("2024-01-01T10:00:00Z"), Instant.parse("2024-01-01T10:00:00Z")
        );
        when(customerService.create(any())).thenReturn(minimal);

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest("maria@example.com");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/customers/" + existingId)))
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist());
    }

    @Test
    void createInternal_shouldReturn400OnInvalidEmail() throws Exception {
        CreateCustomerInternalRequest invalid = new CreateCustomerInternalRequest("not-an-email");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createInternal_shouldReturn400OnBlankEmail() throws Exception {
        CreateCustomerInternalRequest invalid = new CreateCustomerInternalRequest("");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createInternal_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateCustomerException("email", "maria@example.com"));

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest("maria@example.com");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"));
    }

    @Test
    void updateProfile_shouldReturnUpdatedCustomer() throws Exception {
        when(customerService.update(eq(existingId), any())).thenReturn(existing);

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", "12345678901", "11999998888");

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.name").value("Maria"));
    }

    @Test
    void updateProfile_shouldReturn400OnInvalidPayload() throws Exception {
        UpdateProfileRequest invalid = new UpdateProfileRequest(null, "123", "abc");

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpf").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void updateProfile_shouldReturn404WhenMissing() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new CustomerNotFoundException(existingId));

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", null, null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_shouldReturn422WhenChangingCpf() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new ImmutableFieldException("cpf"));

        UpdateProfileRequest request = new UpdateProfileRequest(null, "99999999999", null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"));
    }

    @Test
    void updateProfile_shouldReturn409WhenCpfAlreadyTaken() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new DuplicateCustomerException("cpf", "12345678901"));

        UpdateProfileRequest request = new UpdateProfileRequest(null, "12345678901", null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"));
    }

    @Test
    void updateProfile_shouldReturn409WhenPhoneAlreadyTaken() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new DuplicateCustomerException("phone", "11900000000"));

        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "11900000000");

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"));
    }

    @Test
    void delete_shouldReturn204WhenSuccess() throws Exception {
        doNothing().when(customerService).delete(existingId);

        mockMvc.perform(delete("/customers/{id}", existingId))
                .andExpect(status().isNoContent());

        verify(customerService).delete(existingId);
    }

    @Test
    void delete_shouldReturn404WhenCustomerNotFound() throws Exception {
        doThrow(new CustomerNotFoundException(existingId)).when(customerService).delete(existingId);

        mockMvc.perform(delete("/customers/{id}", existingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void shouldReturn500WithProblemDetailOnUnexpectedException() throws Exception {
        when(customerService.update(eq(existingId), any())).thenThrow(new RuntimeException("boom"));

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", null, null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

}
