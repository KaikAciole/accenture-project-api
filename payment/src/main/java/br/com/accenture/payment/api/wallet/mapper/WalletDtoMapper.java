package br.com.accenture.payment.api.wallet.mapper;

import br.com.accenture.payment.api.wallet.dto.response.WalletResponse;
import br.com.accenture.payment.api.wallet.dto.response.WalletTransactionResponse;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;

public class WalletDtoMapper {

    private WalletDtoMapper() {
    }

    public static WalletResponse toResponse(Wallet wallet) {
        if (wallet == null) {
            return null;
        }

        return new WalletResponse(
                wallet.getId(),
                wallet.getOwnerId(),
                wallet.getOwnerType(),
                wallet.getBalance(),
                wallet.getCreatedAt(),
                wallet.getUpdatedAt()
        );
    }

    public static WalletTransactionResponse toResponse(WalletTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        return new WalletTransactionResponse(
                transaction.getId(),
                transaction.getWalletId(),
                transaction.getPaymentId(),
                transaction.getType(),
                transaction.getReason(),
                transaction.getAmount(),
                transaction.getCreatedAt()
        );
    }

    public static PageResult<WalletTransactionResponse> toTransactionPageResponse(
            PageResult<WalletTransaction> pageResult
    ) {
        if (pageResult == null) {
            return null;
        }

        return pageResult.map(WalletDtoMapper::toResponse);
    }
}
