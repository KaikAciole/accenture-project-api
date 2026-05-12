package br.com.accenture.payment.application.service.wallet;

import br.com.accenture.payment.domain.pagination.PageRequest;
import br.com.accenture.payment.domain.pagination.PageResult;
import br.com.accenture.payment.domain.wallet.enums.WalletOwnerType;
import br.com.accenture.payment.domain.wallet.enums.WalletTransactionReason;
import br.com.accenture.payment.domain.wallet.exception.DuplicateWalletException;
import br.com.accenture.payment.domain.wallet.exception.WalletNotFoundException;
import br.com.accenture.payment.domain.wallet.model.Wallet;
import br.com.accenture.payment.domain.wallet.model.WalletTransaction;
import br.com.accenture.payment.domain.wallet.repository.WalletRepository;
import br.com.accenture.payment.domain.wallet.repository.WalletTransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class WalletService {

    private final WalletRepository walletRepository;
    private final WalletTransactionRepository walletTransactionRepository;

    public WalletService(
            WalletRepository walletRepository,
            WalletTransactionRepository walletTransactionRepository
    ) {
        this.walletRepository = walletRepository;
        this.walletTransactionRepository = walletTransactionRepository;
    }

    @Transactional
    public Wallet create(UUID ownerId, WalletOwnerType ownerType) {
        if (walletRepository.existsByOwnerIdAndOwnerType(ownerId, ownerType)) {
            throw new DuplicateWalletException(ownerId, ownerType);
        }

        Wallet wallet = Wallet.createNew(ownerId, ownerType);

        return walletRepository.save(wallet);
    }

    @Transactional(readOnly = true)
    public Wallet findById(UUID id) {
        return walletRepository.findById(id)
                .orElseThrow(() -> new WalletNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public Wallet findByOwner(UUID ownerId, WalletOwnerType ownerType) {
        return walletRepository.findByOwnerIdAndOwnerType(ownerId, ownerType)
                .orElseThrow(() -> WalletNotFoundException.byOwner(ownerId, ownerType));
    }

    @Transactional(readOnly = true)
    public PageResult<WalletTransaction> findTransactions(UUID walletId, PageRequest pageRequest) {
        findById(walletId);

        return walletTransactionRepository.findByWalletId(walletId, pageRequest);
    }

    @Transactional
    public Wallet credit(
            UUID walletId,
            BigDecimal amount,
            WalletTransactionReason reason,
            UUID paymentId
    ) {
        Wallet wallet = findById(walletId);

        wallet.credit(amount);

        Wallet savedWallet = walletRepository.save(wallet);

        walletTransactionRepository.save(
                WalletTransaction.credit(
                        savedWallet.getId(),
                        paymentId,
                        amount,
                        reason
                )
        );

        return savedWallet;
    }

    @Transactional
    public Wallet debit(
            UUID walletId,
            BigDecimal amount,
            WalletTransactionReason reason,
            UUID paymentId
    ) {
        Wallet wallet = findById(walletId);

        wallet.debit(amount);

        Wallet savedWallet = walletRepository.save(wallet);

        walletTransactionRepository.save(
                WalletTransaction.debit(
                        savedWallet.getId(),
                        paymentId,
                        amount,
                        reason
                )
        );

        return savedWallet;
    }

    @Transactional
    public void transfer(
            UUID fromOwnerId,
            WalletOwnerType fromOwnerType,
            UUID toOwnerId,
            WalletOwnerType toOwnerType,
            BigDecimal amount,
            UUID paymentId
    ) {
        Wallet fromWallet = findByOwner(fromOwnerId, fromOwnerType);
        Wallet toWallet = findByOwner(toOwnerId, toOwnerType);

        fromWallet.debit(amount);
        toWallet.credit(amount);

        Wallet savedFromWallet = walletRepository.save(fromWallet);
        Wallet savedToWallet = walletRepository.save(toWallet);

        walletTransactionRepository.save(
                WalletTransaction.debit(
                        savedFromWallet.getId(),
                        paymentId,
                        amount,
                        WalletTransactionReason.PAYMENT
                )
        );

        walletTransactionRepository.save(
                WalletTransaction.credit(
                        savedToWallet.getId(),
                        paymentId,
                        amount,
                        WalletTransactionReason.SALE_RECEIVED
                )
        );
    }
}