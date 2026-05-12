
package br.com.accenture.payment.domain.exception.wallet;

public class InsufficientWalletBalanceException extends RuntimeException {

    public InsufficientWalletBalanceException(String message) {
        super(message);
    }
}