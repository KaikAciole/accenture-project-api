package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTopUpStatus;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
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

class WalletTopUpTransactionServiceTest {

    private static final UUID WALLET_ID = TestFixtures.WALLET_ID;
    private static final UUID CUSTOMER_ID = TestFixtures.CUSTOMER_ID;
    private static final UUID TOP_UP_ID = UUID.fromString("a1234567-89ab-cdef-0123-456789abcdef");
    private static final BigDecimal AMOUNT = new BigDecimal("80.00");
    private static final BigDecimal FIXED_CHARGE = new BigDecimal("80.00");

    @Test
    void createPendingTopUpLoadsWalletAndPersistsPendingTopUp() {
        FakeWalletRepository walletRepository = new FakeWalletRepository();
        FakeWalletTransactionRepository walletTransactionRepository = new FakeWalletTransactionRepository();
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        Wallet wallet = TestFixtures.walletWithBalance();
        walletRepository.walletById = Optional.of(wallet);
        WalletService walletService = new WalletService(walletRepository, walletTransactionRepository);
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(walletService, topUpRepository, FIXED_CHARGE);

        WalletTopUp result = service.createPendingTopUp(WALLET_ID, CUSTOMER_ID, AMOUNT);

        assertThat(topUpRepository.savedTopUps).hasSize(1);
        assertThat(result.getStatus()).isEqualTo(WalletTopUpStatus.PENDING);
        assertThat(result.getWalletId()).isEqualTo(wallet.getId());
        assertThat(result.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(result.getAmount()).isEqualByComparingTo(AMOUNT);
    }

    @Test
    void attachExternalOrderUpdatesTopUpWhenItExists() {
        FakeWalletRepository walletRepository = new FakeWalletRepository();
        FakeWalletTransactionRepository walletTransactionRepository = new FakeWalletTransactionRepository();
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.of(pendingTopUp());
        WalletService walletService = new WalletService(walletRepository, walletTransactionRepository);
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(walletService, topUpRepository, FIXED_CHARGE);

        WalletTopUp result = service.attachExternalOrder(TOP_UP_ID, "ext-1", "token-1");

        assertThat(result.getExternalOrderId()).isEqualTo("ext-1");
        assertThat(result.getClientToken()).isEqualTo("token-1");
        assertThat(topUpRepository.savedTopUps).hasSize(1);
    }

    @Test
    void attachExternalOrderThrowsWhenTopUpIsMissing() {
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.empty();
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(null, topUpRepository, FIXED_CHARGE);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.attachExternalOrder(TOP_UP_ID, "ext-1", "token-1"))
                .withMessageContaining(TOP_UP_ID.toString());
    }

    @Test
    void approveTopUpAndCreditWalletCreditsWalletAndApprovesTopUp() {
        FakeWalletRepository walletRepository = new FakeWalletRepository();
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());
        FakeWalletTransactionRepository walletTransactionRepository = new FakeWalletTransactionRepository();
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.of(pendingTopUp());
        WalletService walletService = new WalletService(walletRepository, walletTransactionRepository);
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(walletService, topUpRepository, FIXED_CHARGE);

        service.approveTopUpAndCreditWallet(TOP_UP_ID, AMOUNT);

        assertThat(walletRepository.savedWallets).hasSize(1);
        assertThat(walletTransactionRepository.savedTransactions).hasSize(1);
        assertThat(walletTransactionRepository.savedTransactions.getFirst().getReason()).isEqualTo(WalletTransactionReason.TOP_UP);
        assertThat(topUpRepository.savedTopUps)
                .singleElement()
                .satisfies(t -> assertThat(t.getStatus()).isEqualTo(WalletTopUpStatus.APPROVED));
    }

    @Test
    void approveTopUpAndCreditWalletIsIdempotentWhenAlreadyApproved() {
        FakeWalletRepository walletRepository = new FakeWalletRepository();
        FakeWalletTransactionRepository walletTransactionRepository = new FakeWalletTransactionRepository();
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.of(WalletTopUp.restore(
                TOP_UP_ID,
                WALLET_ID,
                CUSTOMER_ID,
                AMOUNT,
                WalletTopUpStatus.APPROVED,
                "ext-1",
                "token-1",
                Instant.now(),
                Instant.now(),
                Instant.now()
        ));
        WalletService walletService = new WalletService(walletRepository, walletTransactionRepository);
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(walletService, topUpRepository, FIXED_CHARGE);

        service.approveTopUpAndCreditWallet(TOP_UP_ID, AMOUNT);

        assertThat(walletRepository.savedWallets).isEmpty();
        assertThat(walletTransactionRepository.savedTransactions).isEmpty();
        assertThat(topUpRepository.savedTopUps).isEmpty();
    }

    @Test
    void approveTopUpAndCreditWalletThrowsWhenTopUpIsMissing() {
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.empty();
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(null, topUpRepository, FIXED_CHARGE);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.approveTopUpAndCreditWallet(TOP_UP_ID, AMOUNT))
                .withMessageContaining(TOP_UP_ID.toString());
    }

    @Test
    void approveTopUpAndCreditWalletRejectsNullPaidAmount() {
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.of(pendingTopUp());
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(null, topUpRepository, FIXED_CHARGE);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.approveTopUpAndCreditWallet(TOP_UP_ID, null))
                .withMessageContaining("lower than the configured charge amount");
    }

    @Test
    void approveTopUpAndCreditWalletRejectsAmountBelowFixedCharge() {
        FakeWalletTopUpRepository topUpRepository = new FakeWalletTopUpRepository();
        topUpRepository.findByIdResponse = Optional.of(pendingTopUp());
        WalletTopUpTransactionService service = new WalletTopUpTransactionService(null, topUpRepository, FIXED_CHARGE);

        assertThatExceptionOfType(IllegalArgumentException.class)
                .isThrownBy(() -> service.approveTopUpAndCreditWallet(TOP_UP_ID, new BigDecimal("79.99")))
                .withMessageContaining("lower than the configured charge amount");
    }

    private static WalletTopUp pendingTopUp() {
        return WalletTopUp.restore(
                TOP_UP_ID,
                WALLET_ID,
                CUSTOMER_ID,
                AMOUNT,
                WalletTopUpStatus.PENDING,
                null,
                null,
                Instant.now(),
                Instant.now(),
                null
        );
    }

    private static final class FakeWalletRepository implements WalletRepository {

        private Optional<Wallet> walletById = Optional.empty();
        private final List<Wallet> savedWallets = new ArrayList<>();

        @Override
        public Wallet save(Wallet wallet) {
            savedWallets.add(wallet);
            return wallet;
        }

        @Override
        public Optional<Wallet> findById(UUID id) {
            return walletById;
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

    private static final class FakeWalletTransactionRepository implements WalletTransactionRepository {

        private final List<WalletTransaction> savedTransactions = new ArrayList<>();

        @Override
        public WalletTransaction save(WalletTransaction transaction) {
            savedTransactions.add(transaction);
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

    private static final class FakeWalletTopUpRepository implements WalletTopUpRepository {

        private Optional<WalletTopUp> findByIdResponse = Optional.empty();
        private final List<WalletTopUp> savedTopUps = new ArrayList<>();

        @Override
        public WalletTopUp save(WalletTopUp walletTopUp) {
            savedTopUps.add(walletTopUp);
            return walletTopUp;
        }

        @Override
        public Optional<WalletTopUp> findById(UUID id) {
            return findByIdResponse;
        }

        @Override
        public Optional<WalletTopUp> findByExternalOrderId(String externalOrderId) {
            return Optional.empty();
        }
    }
}
