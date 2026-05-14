package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.domain.wallet.exception.DuplicateWalletException;
import br.com.accenture.payment.domain.wallet.exception.InsufficientWalletBalanceException;
import br.com.accenture.payment.domain.wallet.exception.WalletNotFoundException;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletRepository;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WalletServiceTest {

    private final FakeWalletRepository walletRepository = new FakeWalletRepository();
    private final FakeWalletTransactionRepository transactionRepository = new FakeWalletTransactionRepository();
    private final WalletService service = new WalletService(walletRepository, transactionRepository);

    @Test
    void createPersistsWalletWhenOwnerDoesNotHaveWallet() {
        walletRepository.nextSavedWallet = TestFixtures.walletWithBalance();

        Wallet wallet = service.create(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER);

        assertThat(wallet.getId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(walletRepository.existsByOwnerCalls).containsExactly(TestFixtures.OWNER_ID);
        assertThat(walletRepository.savedWallets).hasSize(1);
    }

    @Test
    void createRejectsDuplicateOwnerWallet() {
        walletRepository.existsByOwner = true;

        assertThatExceptionOfType(DuplicateWalletException.class)
                .isThrownBy(() -> service.create(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER))
                .withMessage("Wallet already exists for owner id: " + TestFixtures.OWNER_ID + " and owner type: CUSTOMER");

        assertThat(walletRepository.savedWallets).isEmpty();
    }

    @Test
    void createCustomerWalletIfNotExistsIsIdempotent() {
        walletRepository.existsByOwner = true;

        service.createCustomerWalletIfNotExists(TestFixtures.CUSTOMER_ID);

        assertThat(walletRepository.existsByOwnerCalls).containsExactly(TestFixtures.CUSTOMER_ID);
        assertThat(walletRepository.savedWallets).isEmpty();
    }

    @Test
    void createCustomerWalletIfNotExistsPersistsWalletWhenMissing() {
        walletRepository.existsByOwner = false;

        service.createCustomerWalletIfNotExists(TestFixtures.CUSTOMER_ID);

        assertThat(walletRepository.existsByOwnerCalls).containsExactly(TestFixtures.CUSTOMER_ID);
        assertThat(walletRepository.savedWallets)
                .singleElement()
                .satisfies(wallet -> {
                    assertThat(wallet.getOwnerId()).isEqualTo(TestFixtures.CUSTOMER_ID);
                    assertThat(wallet.getOwnerType()).isEqualTo(WalletOwnerType.CUSTOMER);
                });
    }

    @Test
    void findByIdAndFindByOwnerReturnWalletOrThrowWhenMissing() {
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());
        walletRepository.walletByOwner = Optional.of(TestFixtures.walletWithBalance());

        assertThat(service.findById(TestFixtures.WALLET_ID).getId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(service.findByOwner(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER).getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);

        walletRepository.walletById = Optional.empty();
        walletRepository.walletByOwner = Optional.empty();

        assertThatExceptionOfType(WalletNotFoundException.class)
                .isThrownBy(() -> service.findById(TestFixtures.WALLET_ID))
                .withMessage("Wallet not found with id: " + TestFixtures.WALLET_ID);

        assertThatExceptionOfType(WalletNotFoundException.class)
                .isThrownBy(() -> service.findByOwner(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER))
                .withMessage("Wallet not found for owner id: " + TestFixtures.OWNER_ID + " and owner type: CUSTOMER");
    }

    @Test
    void creditUpdatesWalletAndRegistersCreditTransaction() {
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());

        Wallet wallet = service.credit(
                TestFixtures.WALLET_ID,
                new BigDecimal("50.00"),
                WalletTransactionReason.TOP_UP,
                TestFixtures.PAYMENT_ID
        );

        assertThat(wallet.getBalance()).isEqualByComparingTo("300.00");
        assertThat(walletRepository.savedWallets).hasSize(1);
        assertThat(transactionRepository.savedTransactions).hasSize(1);
        assertThat(transactionRepository.savedTransactions.getFirst().getType()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(transactionRepository.savedTransactions.getFirst().getReason()).isEqualTo(WalletTransactionReason.TOP_UP);
    }

    @Test
    void debitUpdatesWalletAndRegistersDebitTransaction() {
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());

        Wallet wallet = service.debit(
                TestFixtures.WALLET_ID,
                new BigDecimal("25.00"),
                WalletTransactionReason.PAYMENT,
                TestFixtures.PAYMENT_ID
        );

        assertThat(wallet.getBalance()).isEqualByComparingTo("225.00");
        assertThat(transactionRepository.savedTransactions.getFirst().getType()).isEqualTo(WalletTransactionType.DEBIT);
    }

    @Test
    void debitDoesNotSaveWhenBalanceIsInsufficient() {
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());

        assertThatExceptionOfType(InsufficientWalletBalanceException.class)
                .isThrownBy(() -> service.debit(
                        TestFixtures.WALLET_ID,
                        new BigDecimal("300.00"),
                        WalletTransactionReason.PAYMENT,
                        TestFixtures.PAYMENT_ID
                ));

        assertThat(walletRepository.savedWallets).isEmpty();
        assertThat(transactionRepository.savedTransactions).isEmpty();
    }

    @Test
    void transferMovesBalanceBetweenWalletsAndRegistersBothTransactions() {
        walletRepository.walletByOwnerResponses.add(TestFixtures.walletWithBalance());
        walletRepository.walletByOwnerResponses.add(TestFixtures.sellerWalletWithBalance());

        service.transfer(
                TestFixtures.OWNER_ID,
                WalletOwnerType.CUSTOMER,
                TestFixtures.SELLER_ID,
                WalletOwnerType.SELLER,
                new BigDecimal("100.00"),
                TestFixtures.PAYMENT_ID
        );

        assertThat(walletRepository.savedWallets).extracting(Wallet::getBalance)
                .containsExactly(new BigDecimal("150.00"), new BigDecimal("100.00"));
        assertThat(transactionRepository.savedTransactions).extracting(WalletTransaction::getType)
                .containsExactly(WalletTransactionType.DEBIT, WalletTransactionType.CREDIT);
        assertThat(transactionRepository.savedTransactions).extracting(WalletTransaction::getReason)
                .containsExactly(WalletTransactionReason.PAYMENT, WalletTransactionReason.SALE_RECEIVED);
    }

    @Test
    void findTransactionsRequiresExistingWalletAndDelegatesToRepository() {
        PageRequest pageRequest = PageRequest.of(0, 10);
        PageResult<WalletTransaction> page = new PageResult<>(List.of(TestFixtures.walletCreditTransaction()), 0, 10, 1, 1);
        walletRepository.walletById = Optional.of(TestFixtures.walletWithBalance());
        transactionRepository.pageResult = page;

        PageResult<WalletTransaction> result = service.findTransactions(TestFixtures.WALLET_ID, pageRequest);

        assertThat(result).isSameAs(page);
        assertThat(transactionRepository.findByWalletCalls).containsExactly(TestFixtures.WALLET_ID);
    }

    private static final class FakeWalletRepository implements WalletRepository {

        private boolean existsByOwner;
        private Wallet nextSavedWallet;
        private Optional<Wallet> walletById = Optional.empty();
        private Optional<Wallet> walletByOwner = Optional.empty();
        private final List<UUID> existsByOwnerCalls = new ArrayList<>();
        private final List<Wallet> savedWallets = new ArrayList<>();
        private final List<Wallet> walletByOwnerResponses = new ArrayList<>();

        @Override
        public Wallet save(Wallet wallet) {
            savedWallets.add(wallet);
            return nextSavedWallet != null ? nextSavedWallet : wallet;
        }

        @Override
        public Optional<Wallet> findById(UUID id) {
            return walletById;
        }

        @Override
        public Optional<Wallet> findByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            if (!walletByOwnerResponses.isEmpty()) {
                return Optional.of(walletByOwnerResponses.removeFirst());
            }
            return walletByOwner;
        }

        @Override
        public boolean existsByOwnerIdAndOwnerType(UUID ownerId, WalletOwnerType ownerType) {
            existsByOwnerCalls.add(ownerId);
            return existsByOwner;
        }
    }

    private static final class FakeWalletTransactionRepository implements WalletTransactionRepository {

        private PageResult<WalletTransaction> pageResult;
        private final List<WalletTransaction> savedTransactions = new ArrayList<>();
        private final List<UUID> findByWalletCalls = new ArrayList<>();

        @Override
        public WalletTransaction save(WalletTransaction transaction) {
            savedTransactions.add(transaction);
            return transaction;
        }

        @Override
        public PageResult<WalletTransaction> findByWalletId(UUID walletId, PageRequest pageRequest) {
            findByWalletCalls.add(walletId);
            return pageResult;
        }
    }
}
