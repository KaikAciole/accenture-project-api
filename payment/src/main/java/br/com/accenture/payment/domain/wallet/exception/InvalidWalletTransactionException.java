package br.com.accenture.payment.domain.wallet.exception;

public class InvalidWalletTransactionException extends RuntimeException {

    public InvalidWalletTransactionException(String message) {
        super(message);
    }
}