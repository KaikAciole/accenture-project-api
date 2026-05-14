package br.com.accenture.notification.domain.enums;

public enum PaymentMethod {
    CREDIT_CARD("Cartao de credito"),
    DEBIT_CARD("Cartao de debito"),
    PIX("PIX"),
    WALLET("Carteira digital");

    private final String label;

    PaymentMethod(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
