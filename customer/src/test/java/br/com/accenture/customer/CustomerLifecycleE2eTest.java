package br.com.accenture.customer;

import br.com.accenture.customer.api.dto.AddressRequest;
import br.com.accenture.customer.api.dto.CustomerRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

}
