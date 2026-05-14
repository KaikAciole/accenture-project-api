package br.com.accenture.customer;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.CreateCustomerInternalRequest;
import br.com.accenture.customer.api.dto.UpdateProfileRequest;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@Transactional
class CustomerLifecycleE2eTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldCreateMinimalCustomerViaInternalEndpoint() throws Exception {
        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest("maria.e2e@example.com");

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.email").value("maria.e2e@example.com"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist())
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn409WhenCreatingCustomerWithDuplicateEmail() throws Exception {
        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest("ana.e2e@example.com");
        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"))
                .andExpect(jsonPath("$.detail").value("Customer already exists with email: ana.e2e@example.com"));
    }

    @Test
    void shouldCompletePartialProfileViaPatch() throws Exception {
        String customerId = createCustomer("carlos.e2e@example.com");

        UpdateProfileRequest update = new UpdateProfileRequest("Carlos E2E", "12312312312", "11900000041");
        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.name").value("Carlos E2E"))
                .andExpect(jsonPath("$.cpf").value("12312312312"))
                .andExpect(jsonPath("$.phone").value("11900000041"))
                .andExpect(jsonPath("$.email").value("carlos.e2e@example.com"));
    }

    @Test
    void shouldApplyPatchPartiallyIgnoringNullFields() throws Exception {
        String customerId = createCustomer("partial.e2e@example.com");

        UpdateProfileRequest onlyName = new UpdateProfileRequest("Just Name", null, null);
        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlyName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Just Name"))
                .andExpect(jsonPath("$.cpf").doesNotExist())
                .andExpect(jsonPath("$.phone").doesNotExist());
    }

    @Test
    void shouldReturn422WhenChangingAlreadyDefinedCpf() throws Exception {
        String customerId = createCustomer("diana.e2e@example.com");

        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest(null, "32132132132", null))))
                .andExpect(status().isOk());

        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest(null, "99999999999", null))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.detail").value("Field 'cpf' cannot be changed after creation"));
    }

    @Test
    void shouldReturn404WhenPatchingMissingCustomer() throws Exception {
        String randomId = "550e8400-e29b-41d4-a716-446655449999";
        mockMvc.perform(patch("/customers/{id}", randomId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UpdateProfileRequest("X", null, null))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void shouldCreateAddressUnderCustomerAndListIt() throws Exception {
        String customerId = createCustomer("joao.e2e@example.com");

        AddressRequest addressRequest = new AddressRequest(
                "Rua das Flores", "123", "Apto 45", "Centro", "São Paulo", "SP", "01001000"
        );
        mockMvc.perform(post("/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.customerId").value(customerId))
                .andExpect(jsonPath("$.street").value("Rua das Flores"));

        mockMvc.perform(get("/customers/{customerId}/addresses", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].street").value("Rua das Flores"))
                .andExpect(jsonPath("$.content[0].zipCode").value("01001000"));
    }

    private String createCustomer(String email) throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateCustomerInternalRequest(email))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

}
