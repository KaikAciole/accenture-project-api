package br.com.accenture.payment.domain.wallet.model;

import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.exception.InsufficientWalletBalanceException;
import br.com.accenture.payment.domain.wallet.exception.InvalidWalletTransactionException;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

class WalletTest {

    @Test
    void createNewStartsWithZeroBalanceAndOwnerData() {
        Wallet wallet = Wallet.createNew(TestFixtures.OWNER_ID, WalletOwnerType.CUSTOMER);

        assertThat(wallet.getId()).isNull();
        assertThat(wallet.getOwnerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(wallet.getOwnerType()).isEqualTo(WalletOwnerType.CUSTOMER);
        assertThat(wallet.getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(wallet.getCreatedAt()).isNotNull();
        assertThat(wallet.getUpdatedAt()).isNotNull();
    }

    @Test
    void restoreRejectsInvalidOwnerAndBalance() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Wallet.createNew(null, WalletOwnerType.CUSTOMER))
                .withMessage("Wallet owner id is required");

        assertThatIllegalArgumentException()
                .isThrownBy(() -> Wallet.createNew(TestFixtures.OWNER_ID, null))
                .withMessage("Wallet owner type is required");

        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> Wallet.restore(
                        TestFixtures.WALLET_ID,
                        TestFixtures.OWNER_ID,
                        WalletOwnerType.CUSTOMER,
                        new BigDecimal("-1.00"),
                        TestFixtures.CREATED_AT,
                        TestFixtures.UPDATED_AT,
                        1L
                ))
                .withMessage("Wallet balance cannot be negative");
    }

    @Test
    void creditAndDebitUpdateBalance() {
        Wallet wallet = TestFixtures.walletWithBalance();

        wallet.credit(new BigDecimal("50.00"));
        wallet.debit(new BigDecimal("75.00"));

        assertThat(wallet.getBalance()).isEqualByComparingTo("225.00");
        assertThat(wallet.getUpdatedAt()).isAfterOrEqualTo(TestFixtures.UPDATED_AT);
    }

    @Test
    void creditAndDebitRejectInvalidAmounts() {
        Wallet wallet = TestFixtures.walletWithBalance();

        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> wallet.credit(BigDecimal.ZERO))
                .withMessage("Amount must be greater than zero");

        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> wallet.debit(null))
                .withMessage("Amount is required");
    }

    @Test
    void debitRejectsInsufficientBalance() {
        Wallet wallet = TestFixtures.walletWithBalance();

        assertThatExceptionOfType(InsufficientWalletBalanceException.class)
                .isThrownBy(() -> wallet.debit(new BigDecimal("300.00")))
                .withMessage("Insufficient wallet balance to complete this transaction");
    }
}
