package br.com.accenture.payment.domain.wallet.model;

import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.domain.wallet.exception.InvalidWalletTransactionException;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

class WalletTransactionTest {

    @Test
    void creditCreatesCreditTransaction() {
        WalletTransaction transaction = WalletTransaction.credit(
                TestFixtures.WALLET_ID,
                TestFixtures.PAYMENT_ID,
                TestFixtures.AMOUNT,
                WalletTransactionReason.TOP_UP
        );

        assertThat(transaction.getId()).isNull();
        assertThat(transaction.getWalletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(transaction.getPaymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(transaction.getType()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(transaction.getReason()).isEqualTo(WalletTransactionReason.TOP_UP);
        assertThat(transaction.getAmount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(transaction.getCreatedAt()).isNotNull();
    }

    @Test
    void debitCreatesDebitTransaction() {
        WalletTransaction transaction = WalletTransaction.debit(
                TestFixtures.WALLET_ID,
                TestFixtures.PAYMENT_ID,
                TestFixtures.AMOUNT,
                WalletTransactionReason.PAYMENT
        );

        assertThat(transaction.getType()).isEqualTo(WalletTransactionType.DEBIT);
        assertThat(transaction.getReason()).isEqualTo(WalletTransactionReason.PAYMENT);
    }

    @Test
    void restoreKeepsPersistedValues() {
        WalletTransaction transaction = TestFixtures.walletCreditTransaction();

        assertThat(transaction.getId()).isEqualTo(TestFixtures.WALLET_TRANSACTION_ID);
        assertThat(transaction.getCreatedAt()).isEqualTo(TestFixtures.CREATED_AT);
    }

    @Test
    void rejectsInvalidData() {
        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> WalletTransaction.credit(null, TestFixtures.PAYMENT_ID, TestFixtures.AMOUNT, WalletTransactionReason.TOP_UP))
                .withMessage("Wallet id is required");

        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> WalletTransaction.restore(
                        TestFixtures.WALLET_TRANSACTION_ID,
                        TestFixtures.WALLET_ID,
                        TestFixtures.PAYMENT_ID,
                        null,
                        WalletTransactionReason.TOP_UP,
                        TestFixtures.AMOUNT,
                        TestFixtures.CREATED_AT
                ))
                .withMessage("Wallet transaction type is required");

        assertThatExceptionOfType(InvalidWalletTransactionException.class)
                .isThrownBy(() -> WalletTransaction.credit(
                        TestFixtures.WALLET_ID,
                        TestFixtures.PAYMENT_ID,
                        BigDecimal.ZERO,
                        WalletTransactionReason.TOP_UP
                ))
                .withMessage("Amount must be greater than zero");
    }
}
