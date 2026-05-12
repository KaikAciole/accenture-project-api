package br.com.accenture.payment.api.wallet.mapper;

import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionType;
import br.com.accenture.payment.support.TestFixtures;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WalletDtoMapperTest {

    @Test
    void toResponseMapsWallet() {
        var response = WalletDtoMapper.toResponse(TestFixtures.walletWithBalance());

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(response.ownerId()).isEqualTo(TestFixtures.OWNER_ID);
        assertThat(response.ownerType()).isEqualTo(WalletOwnerType.COSTUMER);
        assertThat(response.balance()).isEqualByComparingTo(TestFixtures.WALLET_BALANCE);
        assertThat(response.createdAt()).isEqualTo(TestFixtures.CREATED_AT);
        assertThat(response.updatedAt()).isEqualTo(TestFixtures.UPDATED_AT);
    }

    @Test
    void toResponseMapsWalletTransaction() {
        var response = WalletDtoMapper.toResponse(TestFixtures.walletCreditTransaction());

        assertThat(response.id()).isEqualTo(TestFixtures.WALLET_TRANSACTION_ID);
        assertThat(response.walletId()).isEqualTo(TestFixtures.WALLET_ID);
        assertThat(response.paymentId()).isEqualTo(TestFixtures.PAYMENT_ID);
        assertThat(response.type()).isEqualTo(WalletTransactionType.CREDIT);
        assertThat(response.amount()).isEqualByComparingTo(TestFixtures.AMOUNT);
        assertThat(response.createdAt()).isEqualTo(TestFixtures.CREATED_AT);
    }

    @Test
    void toTransactionPageResponseMapsContentAndMetadata() {
        var page = new PageResult<>(List.of(TestFixtures.walletCreditTransaction()), 2, 5, 11, 3);

        var response = WalletDtoMapper.toTransactionPageResponse(page);

        assertThat(response.content()).hasSize(1);
        assertThat(response.pageNumber()).isEqualTo(2);
        assertThat(response.pageSize()).isEqualTo(5);
        assertThat(response.totalElements()).isEqualTo(11);
        assertThat(response.totalPages()).isEqualTo(3);
    }

    @Test
    void nullInputsReturnNull() {
        assertThat(WalletDtoMapper.toResponse((br.com.accenture.payment.domain.wallet.model.Wallet) null)).isNull();
        assertThat(WalletDtoMapper.toResponse((br.com.accenture.payment.domain.wallet.model.WalletTransaction) null)).isNull();
        assertThat(WalletDtoMapper.toTransactionPageResponse(null)).isNull();
    }
}
