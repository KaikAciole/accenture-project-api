package br.com.accenture.customer;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.CustomerRequest;
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

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CustomerLifecycleE2eTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @PersistenceContext
    private EntityManager entityManager;

    @Test
    void shouldCreateCustomerAndRetrieveItById() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Maria E2E", "maria.e2e@example.com", "11122233344", "secret123", "11900000010"
        );

        MvcResult created = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.cpf").value("11122233344"))
                .andExpect(jsonPath("$.password").doesNotExist())
                .andReturn();

        String customerId = objectMapper.readTree(created.getResponse().getContentAsString())
                .get("id").asString();

        mockMvc.perform(get("/customers/{id}", customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(customerId))
                .andExpect(jsonPath("$.email").value("maria.e2e@example.com"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.updatedAt").exists());
    }

    @Test
    void shouldCreateAddressUnderCustomerAndListIt() throws Exception {
        CustomerRequest customerRequest = new CustomerRequest(
                "João E2E", "joao.e2e@example.com", "55566677788", "secret123", "11900000020"
        );
        MvcResult customerResult = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(customerRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String customerId = objectMapper.readTree(customerResult.getResponse().getContentAsString())
                .get("id").asString();

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

    @Test
    void shouldReturn409WhenCreatingCustomerWithDuplicateCpf() throws Exception {
        CustomerRequest first = new CustomerRequest(
                "Ana E2E", "ana.e2e@example.com", "99988877766", "secret123", "11900000030"
        );
        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(first)))
                .andExpect(status().isCreated());

        CustomerRequest sameCpf = new CustomerRequest(
                "Ana 2", "ana2@example.com", "99988877766", "secret123", "11900000031"
        );
        mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(sameCpf)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate customer"))
                .andExpect(jsonPath("$.detail").value("Customer already exists with cpf: 99988877766"));
    }

    @Test
    void shouldUpdateCustomerMutableFields() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Carlos E2E", "carlos.e2e@example.com", "12312312312", "secret123", "11900000040"
        );
        String customerId = createCustomer(request);

        CustomerRequest updateRequest = new CustomerRequest(
                "Carlos Updated", "carlos.updated@example.com", "12312312312", "newpass12", "11900000041"
        );
        mockMvc.perform(put("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Carlos Updated"))
                .andExpect(jsonPath("$.email").value("carlos.updated@example.com"))
                .andExpect(jsonPath("$.phone").value("11900000041"))
                .andExpect(jsonPath("$.cpf").value("12312312312"))
                .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void shouldReturn422WhenUpdatingCpf() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Diana E2E", "diana.e2e@example.com", "32132132132", "secret123", "11900000050"
        );
        String customerId = createCustomer(request);

        CustomerRequest changingCpf = new CustomerRequest(
                "Diana E2E", "diana.e2e@example.com", "99999999999", "secret123", "11900000050"
        );
        mockMvc.perform(put("/customers/{id}", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(changingCpf)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Immutable field"))
                .andExpect(jsonPath("$.detail").value("Field 'cpf' cannot be changed after creation"));
    }

    @Test
    void shouldDeleteCustomerAndReturn404OnSubsequentGet() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Eduardo E2E", "eduardo.e2e@example.com", "45645645645", "secret123", "11900000060"
        );
        String customerId = createCustomer(request);

        mockMvc.perform(delete("/customers/{id}", customerId))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/customers/{id}", customerId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Customer not found"));
    }

    @Test
    void shouldCascadeDeleteAddressesWhenCustomerIsDeleted() throws Exception {
        CustomerRequest request = new CustomerRequest(
                "Fabio E2E", "fabio.e2e@example.com", "78978978978", "secret123", "11900000070"
        );
        String customerId = createCustomer(request);

        AddressRequest addressRequest = new AddressRequest(
                "Rua das Flores", "123", null, "Centro", "São Paulo", "SP", "01001000"
        );
        MvcResult addressResult = mockMvc.perform(post("/customers/{customerId}/addresses", customerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(addressRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        String addressId = objectMapper.readTree(addressResult.getResponse().getContentAsString())
                .get("id").asString();

        mockMvc.perform(delete("/customers/{id}", customerId))
                .andExpect(status().isNoContent());

        entityManager.flush();
        entityManager.clear();

        mockMvc.perform(get("/customers/{customerId}/addresses/{addressId}", customerId, addressId))
                .andExpect(status().isNotFound());
    }

    private String createCustomer(CustomerRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/customers")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }

}
