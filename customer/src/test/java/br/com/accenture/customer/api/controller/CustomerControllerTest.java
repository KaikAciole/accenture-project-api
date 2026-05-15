package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CreateCustomerInternalRequest;
import br.com.accenture.customer.api.dto.UpdateProfileRequest;
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
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CustomerController.class)
@AutoConfigureMockMvc(addFilters = false)
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
        when(customerService.create(any())).thenReturn(existing);

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest(
                "Maria", "maria@example.com", "12345678901", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", endsWith("/customers/" + existingId)))
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.name").value("Maria"))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.cpf").value("12345678901"))
                .andExpect(jsonPath("$.phone").value("11999998888"));
    }

    @Test
    void createInternal_shouldReturn400OnInvalidEmail() throws Exception {
        CreateCustomerInternalRequest invalid = new CreateCustomerInternalRequest(
                "Maria", "not-an-email", "12345678901", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"))
                .andExpect(jsonPath("$.errors.email").exists());
    }

    @Test
    void createInternal_shouldReturn400OnBlankRequiredFields() throws Exception {
        CreateCustomerInternalRequest invalid = new CreateCustomerInternalRequest("", "", "", "");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.name").exists())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.cpf").exists())
                .andExpect(jsonPath("$.errors.phone").exists());
    }

    @Test
    void createInternal_shouldReturn400OnInvalidCpfPattern() throws Exception {
        CreateCustomerInternalRequest invalid = new CreateCustomerInternalRequest(
                "Maria", "maria@example.com", "abc", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.cpf").exists());
    }

    @Test
    void createInternal_shouldReturn409WhenEmailAlreadyExists() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateCustomerException("email", "maria@example.com"));

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest(
                "Maria", "maria@example.com", "12345678901", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"));
    }

    @Test
    void createInternal_shouldReturn409WhenCpfAlreadyExists() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateCustomerException("cpf", "12345678901"));

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest(
                "Maria", "maria@example.com", "12345678901", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Customer already exists with cpf: 12345678901"));
    }

    @Test
    void createInternal_shouldReturn409WhenPhoneAlreadyExists() throws Exception {
        when(customerService.create(any()))
                .thenThrow(new DuplicateCustomerException("phone", "11999998888"));

        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest(
                "Maria", "maria@example.com", "12345678901", "11999998888"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Customer already exists with phone: 11999998888"));
    }

    @Test
    void findByIdInternal_shouldReturn200WithCustomerData() throws Exception {
        when(customerService.findById(existingId)).thenReturn(existing);

        mockMvc.perform(get("/internal/customers/{customerId}", existingId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.email").value("maria@example.com"))
                .andExpect(jsonPath("$.name").value("Maria"));
    }

    @Test
    void findByIdInternal_shouldReturn404WhenCustomerNotFound() throws Exception {
        when(customerService.findById(existingId))
                .thenThrow(new CustomerNotFoundException(existingId));

        mockMvc.perform(get("/internal/customers/{customerId}", existingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void list_shouldReturnPagedResults() throws Exception {
        PageResult<Customer> page = new PageResult<>(List.of(existing), 0, 10, 1, 1);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(existingId.toString()))
                .andExpect(jsonPath("$.content[0].email").value("maria@example.com"))
                .andExpect(jsonPath("$.content[0].name").value("Maria"))
                .andExpect(jsonPath("$.pageNumber").value(0))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.hasNext").value(false))
                .andExpect(jsonPath("$.hasPrevious").value(false));
    }

    @Test
    void list_shouldReturnEmptyPageWhenNoCustomers() throws Exception {
        PageResult<Customer> empty = new PageResult<>(List.of(), 0, 20, 0, 0);
        when(customerService.findAll(any(PageRequest.class))).thenReturn(empty);

        mockMvc.perform(get("/customers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void updateProfile_shouldReturnUpdatedCustomer() throws Exception {
        when(customerService.update(eq(existingId), any())).thenReturn(existing);

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", null, "12345678901", "11999998888");

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(existingId.toString()))
                .andExpect(jsonPath("$.name").value("Maria"));
    }

    @Test
    void updateProfile_shouldReturn400OnInvalidPayload() throws Exception {
        UpdateProfileRequest invalid = new UpdateProfileRequest(null, null, "123", "abc");

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

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", null, null, null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound());
    }

    @Test
    void updateProfile_shouldReturn422WhenChangingCpf() throws Exception {
        when(customerService.update(eq(existingId), any()))
                .thenThrow(new ImmutableFieldException("cpf"));

        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "99999999999", null);

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

        UpdateProfileRequest request = new UpdateProfileRequest(null, null, "12345678901", null);

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

        UpdateProfileRequest request = new UpdateProfileRequest(null, null, null, "11900000000");

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

        UpdateProfileRequest request = new UpdateProfileRequest("Maria", null, null, null);

        mockMvc.perform(patch("/customers/{id}", existingId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.title").value("Internal server error"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

}
