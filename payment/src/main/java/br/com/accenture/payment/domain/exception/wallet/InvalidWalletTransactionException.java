package br.com.accenture.payment.domain.exception.wallet;

public class InvalidWalletTransactionException extends RuntimeException {

    public InvalidWalletTransactionException(String message) {
        super(message);
    }
}