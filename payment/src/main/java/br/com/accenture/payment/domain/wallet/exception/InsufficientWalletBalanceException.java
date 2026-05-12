
package br.com.accenture.payment.domain.wallet.exception;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}