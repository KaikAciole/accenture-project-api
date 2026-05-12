package br.com.accenture.payment.e2e.wallet;

import br.com.accenture.payment.api.wallet.dto.request.WalletCreateRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletCreditRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletDebitRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletTransferRequest;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.infrastructure.persistence.wallet.WalletJpaRepository;
import br.com.accenture.payment.infrastructure.persistence.wallet.WalletTransactionJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class WalletLifecycleE2eTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private WalletJpaRepository walletJpaRepository;

    @Autowired
    private WalletTransactionJpaRepository walletTransactionJpaRepository;

    @Test
    void shouldCreateWalletAndRetrieveItByIdAndOwner() throws Exception {
        UUID ownerId = UUID.randomUUID();

        String walletId = createWallet(ownerId, WalletOwnerType.COSTUMER);

        mockMvc.perform(get("/wallets/{id}", walletId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId))
                .andExpect(jsonPath("$.ownerId").value(ownerId.toString()))
                .andExpect(jsonPath("$.balance").value(0));

        mockMvc.perform(get("/wallets/owners/{ownerType}/{ownerId}", WalletOwnerType.COSTUMER, ownerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(walletId));
    }

    @Test
    void shouldReturn409WhenCreatingDuplicateOwnerWallet() throws Exception {
        UUID ownerId = UUID.randomUUID();
        WalletCreateRequest request = new WalletCreateRequest(ownerId, WalletOwnerType.COSTUMER);
        createWallet(request);

        mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Duplicate wallet"));
    }

    @Test
    void shouldCreditDebitAndListWalletTransactions() throws Exception {
        String walletId = createWallet(UUID.randomUUID(), WalletOwnerType.COSTUMER);
        UUID paymentId = UUID.randomUUID();

        mockMvc.perform(patch("/wallets/{walletId}/credit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletCreditRequest(
                                new BigDecimal("200.00"),
                                WalletTransactionReason.TOP_UP,
                                paymentId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(200.00));

        mockMvc.perform(patch("/wallets/{walletId}/debit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletDebitRequest(
                                new BigDecimal("50.00"),
                                WalletTransactionReason.PAYMENT,
                                paymentId
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(150.00));

        mockMvc.perform(get("/wallets/{walletId}/transactions", walletId)
                        .param("size", "10")
                        .param("sort", "amount,desc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].amount").value(200.00))
                .andExpect(jsonPath("$.content[0].type").value("CREDIT"))
                .andExpect(jsonPath("$.content[1].amount").value(50.00))
                .andExpect(jsonPath("$.content[1].type").value("DEBIT"));
    }

    @Test
    void shouldTransferBalanceBetweenWallets() throws Exception {
        UUID customerId = UUID.randomUUID();
        UUID sellerId = UUID.randomUUID();
        createWallet(customerId, WalletOwnerType.COSTUMER);
        createWallet(sellerId, WalletOwnerType.SELLER);

        String customerWalletId = walletJpaRepository
                .findByOwnerIdAndOwnerType(customerId, WalletOwnerType.COSTUMER)
                .orElseThrow()
                .getId()
                .toString();

        mockMvc.perform(patch("/wallets/{walletId}/credit", customerWalletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletCreditRequest(
                                new BigDecimal("120.00"),
                                WalletTransactionReason.TOP_UP,
                                UUID.randomUUID()
                        ))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/wallets/transfer")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletTransferRequest(
                                customerId,
                                WalletOwnerType.COSTUMER,
                                sellerId,
                                WalletOwnerType.SELLER,
                                new BigDecimal("80.00"),
                                UUID.randomUUID()
                        ))))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/wallets/owners/{ownerType}/{ownerId}", WalletOwnerType.COSTUMER, customerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(40.00));

        mockMvc.perform(get("/wallets/owners/{ownerType}/{ownerId}", WalletOwnerType.SELLER, sellerId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance").value(80.00));

        assertThat(walletTransactionJpaRepository.findAll()).hasSize(3);
    }

    @Test
    void shouldReturn422ForInsufficientBalanceAnd400ForInvalidPayload() throws Exception {
        String walletId = createWallet(UUID.randomUUID(), WalletOwnerType.COSTUMER);

        mockMvc.perform(patch("/wallets/{walletId}/debit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletDebitRequest(
                                new BigDecimal("1.00"),
                                WalletTransactionReason.PAYMENT,
                                UUID.randomUUID()
                        ))))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Insufficient wallet balance"));

        mockMvc.perform(patch("/wallets/{walletId}/credit", walletId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new WalletCreditRequest(
                                BigDecimal.ZERO,
                                WalletTransactionReason.TOP_UP,
                                UUID.randomUUID()
                        ))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Validation error"));
    }

    private String createWallet(UUID ownerId, WalletOwnerType ownerType) throws Exception {
        return createWallet(new WalletCreateRequest(ownerId, ownerType));
    }

    private String createWallet(WalletCreateRequest request) throws Exception {
        MvcResult result = mockMvc.perform(post("/wallets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        assertThat(walletJpaRepository.existsByOwnerIdAndOwnerType(request.ownerId(), request.ownerType())).isTrue();
        return objectMapper.readTree(result.getResponse().getContentAsString())
                .get("id").asString();
    }
}
