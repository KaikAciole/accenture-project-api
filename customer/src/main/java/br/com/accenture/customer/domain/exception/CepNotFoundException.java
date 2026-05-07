package br.com.accenture.customer.domain.exception;

public class CepNotFoundException extends RuntimeException {

    public CepNotFoundException(String cep) {
        super("CEP not found: " + cep);
    }

}
