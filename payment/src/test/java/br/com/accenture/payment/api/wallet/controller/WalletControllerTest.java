package br.com.accenture.payment.api.wallet.controller;

import br.com.accenture.payment.api.wallet.dto.request.WalletCreateRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletCreditRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletDebitRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletTopUpRequest;
import br.com.accenture.payment.api.wallet.dto.request.WalletTransferRequest;
import br.com.accenture.payment.api.wallet.dto.response.TopUpSubmitResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletTopUpResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletTransactionResponse;
import br.com.accenture.payment.application.port.WalletTopUpGateway;
import br.com.accenture.payment.application.service.wallet.WalletService;
import br.com.accenture.payment.application.service.wallet.WalletTopUpService;
import br.com.accenture.payment.application.service.wallet.WalletTopUpTransactionService;
import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.exception.WalletNotFoundException;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletRepository;
import br.com.accenture.payment.domain.wallet.repository.WalletTopUpRepository;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WalletControllerTest {

    private final FakeWalletService walletService = new FakeWalletService();
    private final FakeWalletTopUpService walletTopUpService = new FakeWalletTopUpService();
    private final WalletController controller = new WalletController(walletService, walletTopUpService);

    @Test
    void createReturnsCreatedWallet() {
        walletService.createResult = TestFixtures.walletWithBalance();

        WalletResponse response = controller.create(new WalletCreateRequest(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER));

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(walletService.createCalls)
                .containsExactly(new CreateCall(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER));
    }

    @Test
    void findByIdReturnsWallet() {
        walletService.findByIdResult = TestFixtures.walletWithBalance();

        WalletResponse response = controller.findById(TestFixtures.WALLET_ID);

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(walletService.findByIdCalls).containsExactly(TestFixtures.WALLET_ID);
    }

    @Test
    void findByOwnerReturnsWallet() {
        walletService.findByOwnerResult = TestFixtures.walletWithBalance();

        WalletResponse response = controller.findByOwner(WalletOwnerType.CUSTOMER, TestFixtures.OWNER_ID);

        assertThat(response.ownerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(walletService.findByOwnerCalls)
                .containsExactly(new FindByOwnerCall(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER));
    }

    @Test
    void findTransactionsReturnsPagedTransactions() {
        walletService.findTransactionsResult = new PageResult<>(
                List.of(TestFixtures.walletCreditTransaction()), 0, 10, 1, 1
        );

        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);
        PageResult<WalletTransactionResponse> response = controller.findTransactions(TestFixtures.WALLET_ID, pageable);

        assertThat(response.content()).hasSize(1);
        assertThat(response.totalElements()).isEqualTo(1);
        assertThat(walletService.findTransactionsCalls).hasSize(1);
        assertThat(walletService.findTransactionsCalls.getFirst().walletId()).isEqualTo(TestFixtures.WALLET_ID);
    }

    @Test
    void findTransactionsByOwnerAllowsAdminAccessToAnyCustomer() {
        walletService.findByOwnerResult = TestFixtures.walletWithBalance();
        walletService.findTransactionsResult = new PageResult<>(
                List.of(TestFixtures.walletCreditTransaction()), 0, 10, 1, 1
        );
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        PageResult<WalletTransactionResponse> response = controller.findTransactionsByOwner(
                WalletOwnerType.CUSTOMER,
                TestFixtures.OWNER_ID,
                UUID.randomUUID(),
                "ADMIN",
                pageable
        );

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void findTransactionsByOwnerAllowsCustomerToAccessOwnWallet() {
        walletService.findByOwnerResult = TestFixtures.walletWithBalance();
        walletService.findTransactionsResult = new PageResult<>(
                List.of(TestFixtures.walletCreditTransaction()), 0, 10, 1, 1
        );
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        PageResult<WalletTransactionResponse> response = controller.findTransactionsByOwner(
                WalletOwnerType.CUSTOMER,
                TestFixtures.OWNER_ID,
                TestFixtures.OWNER_ID,
                "CUSTOMER",
                pageable
        );

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void findTransactionsByOwnerSkipsValidationWhenCustomerHeaderIsAbsent() {
        walletService.findByOwnerResult = TestFixtures.walletWithBalance();
        walletService.findTransactionsResult = new PageResult<>(
                List.of(TestFixtures.walletCreditTransaction()), 0, 10, 1, 1
        );
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        PageResult<WalletTransactionResponse> response = controller.findTransactionsByOwner(
                WalletOwnerType.COMPANY,
                TestFixtures.OWNER_ID,
                null,
                null,
                pageable
        );

        assertThat(response.content()).hasSize(1);
    }

    @Test
    void findTransactionsByOwnerRejectsCustomerLookingAtDifferentOwner() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        assertThatExceptionOfType(WalletNotFoundException.class)
                .isThrownBy(() -> controller.findTransactionsByOwner(
                        WalletOwnerType.CUSTOMER,
                        TestFixtures.OWNER_ID,
                        UUID.randomUUID(),
                        "CUSTOMER",
                        pageable
                ));
    }

    @Test
    void findTransactionsByOwnerRejectsCustomerAskingForCompanyWallet() {
        var pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        assertThatExceptionOfType(WalletNotFoundException.class)
                .isThrownBy(() -> controller.findTransactionsByOwner(
                        WalletOwnerType.COMPANY,
                        TestFixtures.OWNER_ID,
                        TestFixtures.OWNER_ID,
                        "CUSTOMER",
                        pageable
                ));
    }

    @Test
    void startTopUpReturnsTopUpResponse() {
        WalletTopUp topUp = pendingTopUp();
        walletTopUpService.createPendingResult = topUp;

        WalletTopUpResponse response = controller.startTopUp(
                TestFixtures.WALLET_ID,
                new WalletTopUpRequest(TestFixtures.CUSTOMER_ID, new BigDecimal("80.00"), "client@example.com")
        );

        assertThat(response.id()).isEqualTo(topUp.getId());
        assertThat(response.status()).isEqualTo(WalletTopUpStatus.PENDING.name());
        assertThat(walletTopUpService.createPendingCalls)
                .containsExactly(new CreatePendingCall(TestFixtures.WALLET_ID, TestFixtures.CUSTOMER_ID, new BigDecimal("80.00")));
    }

    @Test
    void submitTopUpReturnsExternalOrderAndQrCode() {
        WalletTopUp topUp = pendingTopUp();
        topUp.attachExternalOrder("ext-1", "token-1");
        walletTopUpService.submitResult = new WalletTopUpService.TopUpSubmissionResult(
                topUp, "qr", "qr64", "ticket"
        );

        TopUpSubmitResponse response = controller.submitTopUp(topUp.getId());

        assertThat(response.topUpId()).isEqualTo(topUp.getId());
        assertThat(response.externalOrderId()).isEqualTo("ext-1");
        assertThat(response.status()).isEqualTo(WalletTopUpStatus.PENDING.name());
        assertThat(response.qrCode()).isEqualTo("qr");
        assertThat(response.qrCodeBase64()).isEqualTo("qr64");
        assertThat(response.ticketUrl()).isEqualTo("ticket");
        assertThat(walletTopUpService.submitCalls).containsExactly(topUp.getId());
    }

    @Test
    void creditDelegatesToService() {
        walletService.creditResult = TestFixtures.walletWithBalance();

        WalletResponse response = controller.credit(
                TestFixtures.WALLET_ID,
                new WalletCreditRequest(new BigDecimal("50.00"), WalletTransactionReason.TOP_UP, TestFixtures.PAYMENT_ID)
        );

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(walletService.creditCalls)
                .containsExactly(new CreditCall(TestFixtures.WALLET_ID, new BigDecimal("50.00"), WalletTransactionReason.TOP_UP, TestFixtures.PAYMENT_ID));
    }

    @Test
    void debitDelegatesToService() {
        walletService.debitResult = TestFixtures.walletWithBalance();

        WalletResponse response = controller.debit(
                TestFixtures.WALLET_ID,
                new WalletDebitRequest(new BigDecimal("25.00"), WalletTransactionReason.PAYMENT, TestFixtures.PAYMENT_ID)
        );

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(walletService.debitCalls)
                .containsExactly(new CreditCall(TestFixtures.WALLET_ID, new BigDecimal("25.00"), WalletTransactionReason.PAYMENT, TestFixtures.PAYMENT_ID));
    }

    @Test
    void transferDelegatesToService() {
        controller.transfer(new WalletTransferRequest(
                TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER,
                TestFixtures.SELLER_ID, WalletOwnerType.SELLER,
                new BigDecimal("100.00"),
                TestFixtures.PAYMENT_ID
        ));

        assertThat(walletService.transferCalls).hasSize(1);
        assertThat(walletService.transferCalls.getFirst().amount()).isEqualByComparingTo("100.00");
        assertThat(walletService.transferCalls.getFirst().fromOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(walletService.transferCalls.getFirst().toOwnerId()).isEqualTo(TestFixtures.SELLER_ID);
    }

    private static WalletTopUp pendingTopUp() {
        return WalletTopUp.restore(
                UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef"),
                TestFixtures.WALLET_ID,
                TestFixtures.CUSTOMER_ID,
                new BigDecimal("80.00"),
                WalletTopUpStatus.PENDING,
                null,
                null,
                Instant.parse("2026-05-09T10:00:00Z"),
                Instant.parse("2026-05-09T10:05:00Z"),
                null
        );
    }

    private record CreateCall(UUID ownerId, WalletOwnerType ownerType) {
    }

    private record FindByOwnerCall(UUID ownerId, WalletOwnerType ownerType) {
    }

    private record FindTransactionsCall(UUID walletId, PageRequest pageRequest) {
    }

    private record CreditCall(UUID walletId, BigDecimal amount, WalletTransactionReason reason, UUID paymentId) {
    }

    private record CreatePendingCall(UUID walletId, UUID customerId, BigDecimal amount) {
    }

    private record TransferCall(
            UUID fromOwnerId, WalletOwnerType fromOwnerType,
            UUID toOwnerId, WalletOwnerType toOwnerType,
            BigDecimal amount, UUID paymentId
    ) {
    }

    private static final class FakeWalletService extends WalletService {

        private Wallet createResult;
        private Wallet findByIdResult;
        private Wallet findByOwnerResult;
        private Wallet creditResult;
        private Wallet debitResult;
        private PageResult<WalletTransaction> findTransactionsResult;
        private final List<CreateCall> createCalls = new ArrayList<>();
        private final List<UUID> findByIdCalls = new ArrayList<>();
        private final List<FindByOwnerCall> findByOwnerCalls = new ArrayList<>();
        private final List<FindTransactionsCall> findTransactionsCalls = new ArrayList<>();
        private final List<CreditCall> creditCalls = new ArrayList<>();
        private final List<CreditCall> debitCalls = new ArrayList<>();
        private final List<TransferCall> transferCalls = new ArrayList<>();

        private FakeWalletService() {
            super(new NoopWalletRepository(), new NoopWalletTransactionRepository());
        }

        @Override
        public Wallet create(UUID ownerId, WalletOwnerType ownerType) {
            createCalls.add(new CreateCall(ownerId, ownerType));
            return createResult;
        }

        @Override
        public Wallet findById(UUID id) {
            findByIdCalls.add(id);
            return findByIdResult;
        }

        @Override
        public Wallet findByOwner(UUID ownerId, WalletOwnerType ownerType) {
            findByOwnerCalls.add(new FindByOwnerCall(ownerId, ownerType));
            return findByOwnerResult;
        }

        @Override
        public PageResult<WalletTransaction> findTransactions(UUID walletId, PageRequest pageRequest) {
            findTransactionsCalls.add(new FindTransactionsCall(walletId, pageRequest));
            return findTransactionsResult;
        }

        @Override
        public Wallet credit(UUID walletId, BigDecimal amount, WalletTransactionReason reason, UUID paymentId) {
            creditCalls.add(new CreditCall(walletId, amount, reason, paymentId));
            return creditResult;
        }

        @Override
        public Wallet debit(UUID walletId, BigDecimal amount, WalletTransactionReason reason, UUID paymentId) {
            debitCalls.add(new CreditCall(walletId, amount, reason, paymentId));
            return debitResult;
        }

        @Override
        public void transfer(UUID fromOwnerId, WalletOwnerType fromOwnerType,
                              UUID toOwnerId, WalletOwnerType toOwnerType,
                              BigDecimal amount, UUID paymentId) {
            transferCalls.add(new TransferCall(fromOwnerId, fromOwnerType, toOwnerId, toOwnerType, amount, paymentId));
        }
    }

    private static final class FakeWalletTopUpService extends WalletTopUpService {

        private WalletTopUp createPendingResult;
        private TopUpSubmissionResult submitResult;
        private final List<CreatePendingCall> createPendingCalls = new ArrayList<>();
        private final List<UUID> submitCalls = new ArrayList<>();

        private FakeWalletTopUpService() {
            super(
                    new NoopTransactionService(),
                    new NoopGateway(),
                    new NoopTopUpRepository()
            );
        }

        @Override
        public WalletTopUp createPendingTopUp(UUID walletId, UUID customerId, BigDecimal amount) {
            createPendingCalls.add(new CreatePendingCall(walletId, customerId, amount));
            return createPendingResult;
        }

        @Override
        public TopUpSubmissionResult submitToMercadoPago(UUID topUpId) {
            submitCalls.add(topUpId);
            return submitResult;
        }
    }

    private static final class NoopWalletRepository implements WalletRepository {
        @Override
        public Wallet save(Wallet wallet) {
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            return Optional.empty();
        }

        @Override
        public boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            return false;
        }
    }

    private static final class NoopWalletTransactionRepository implements WalletTransactionRepository {
        @Override
        public WalletTransaction save(WalletTransaction transaction) {
            return transaction;
        }

        @Override
        public PageResult<WalletTransaction> findByWalletId(UUID walletId, PageRequest pageRequest) {
            return new PageResult<>(List.of(), 0, 10, 0, 0);
        }

        @Override
        public boolean existsByPaymentIdAndReason(UUID paymentId, WalletTransactionReason reason) {
            return false;
        }
    }

    private static final class NoopTopUpRepository implements WalletTopUpRepository {
        @Override
        public WalletTopUp save(WalletTopUp walletTopUp) {
            return walletTopUp;
        }

        @Override
        public Optional<WalletTopUp> findById(UUID id) {
            return Optional.empty();
        }

        @Override
        public Optional<WalletTopUp> findByExternalOrderId(String externalOrderId) {
            return Optional.empty();
        }
    }

    private static final class NoopTransactionService extends WalletTopUpTransactionService {
        private NoopTransactionService() {
            super(null, null, BigDecimal.ZERO);
        }
    }

    private static final class NoopGateway implements WalletTopUpGateway {
        @Override
        public WalletTopUpGatewayResponse createOrder(WalletTopUpGatewayRequest request) {
            return null;
        }
    }
}
