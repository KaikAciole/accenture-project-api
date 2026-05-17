package br.com.accenture.payment.api.wallet.mapper;

import br.com.accenture.payment.api.wallet.dto.response.WalletTopUpResponse;
import br.com.accenture.payment.domain.wallet.model.WalletTopUp;

public class WalletTopUpDtoMapper {

    private WalletTopUpDtoMapper() {
    }

    public static WalletTopUpResponse toResponse(WalletTopUp walletTopUp) {
        return new WalletTopUpResponse(
                walletTopUp.getId(),
                walletTopUp.getWalletId(),
                walletTopUp.getCustomerId(),
                walletTopUp.getAmount(),
                walletTopUp.getStatus().name()
        );
    }
}