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
    void shouldCreateCustomerViaInternalEndpoint() throws Exception {
        CreateCustomerInternalRequest request = new CreateCustomerInternalRequest(
                "Maria E2E", "maria.e2e@example.com", "10000000001", "11900000001"
        );

        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("Maria E2E"))
                .andExpect(jsonPath("$.email").value("maria.e2e@example.com"))
                .andExpect(jsonPath("$.cpf").value("10000000001"))
                .andExpect(jsonPath("$.phone").value("11900000001"))
                .andExpect(jsonPath("$.createdAt").exists());
    }

    @Test
    void shouldReturn409WhenCreatingCustomerWithDuplicateEmail() throws Exception {
        CreateCustomerInternalRequest first = new CreateCustomerInternalRequest(
                "Ana", "ana.e2e@example.com", "10000000002", "11900000002"
        );
        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CreateCustomerInternalRequest duplicate = new CreateCustomerInternalRequest(
                "Ana 2", "ana.e2e@example.com", "10000000003", "11900000003"
        );
        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"))
                .andExpect(jsonPath("$.detail").value("Customer already exists with email: ana.e2e@example.com"));
    }

    @Test
    void shouldReturn409WhenCreatingCustomerWithDuplicateCpf() throws Exception {
        CreateCustomerInternalRequest first = new CreateCustomerInternalRequest(
                "Bruna", "bruna.e2e@example.com", "10000000004", "11900000004"
        );
        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CreateCustomerInternalRequest sameCpf = new CreateCustomerInternalRequest(
                "Bruna 2", "bruna2.e2e@example.com", "10000000004", "11900000005"
        );
        mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameCpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("Customer already exists with cpf: 10000000004"));
    }

    @Test
    void shouldUpdateProfileViaPatch() throws Exception {
        String customerId = createCustomer("Carlos", "carlos.e2e@example.com", "10000000006", "11900000006");

        UpdateProfileRequest update = new UpdateProfileRequest("Carlos Updated", null, "11900000099");
        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(update)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.name").value("Carlos Updated"))
                .andExpect(jsonPath("$.cpf").value("10000000006"))
                .andExpect(jsonPath("$.phone").value("11900000099"))
                .andExpect(jsonPath("$.email").value("carlos.e2e@example.com"));
    }

    @Test
    void shouldApplyPatchPartiallyIgnoringNullFields() throws Exception {
        String customerId = createCustomer("Original Name", "partial.e2e@example.com", "10000000007", "11900000007");

        UpdateProfileRequest onlyName = new UpdateProfileRequest("Just Name", null, null);
        mockMvc.perform(patch("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(onlyName)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Just Name"))
                .andExpect(jsonPath("$.cpf").value("10000000007"))
                .andExpect(jsonPath("$.phone").value("11900000007"));
    }

    @Test
    void shouldReturn422WhenChangingAlreadyDefinedCpf() throws Exception {
        String customerId = createCustomer("Diana", "diana.e2e@example.com", "10000000008", "11900000008");

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
        String customerId = createCustomer("João", "joao.e2e@example.com", "10000000009", "11900000009");

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

    private String createCustomer(String name, String email, String cpf, String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/internal/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                new CreateCustomerInternalRequest(name, email, cpf, phone))))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

}
