package br.com.accenture.customer.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Address {

    private UUID id;
    private UUID customerId;
    private String street;
    private String number;
    private String complement;
    private String neighborhood;
    private String city;
    private String state;
    private String zipCode;
    private Instant createdAt;
    private Instant updatedAt;

    private Address(UUID id,
                    UUID customerId,
                    String street,
                    String number,
                    String complement,
                    String neighborhood,
                    String city,
                    String state,
                    String zipCode,
                    Instant createdAt,
                    Instant updatedAt) {
        this.id = id;
        this.customerId = customerId;
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Address createNew(UUID customerId,
                                    String street,
                                    String number,
                                    String complement,
                                    String neighborhood,
                                    String city,
                                    String state,
                                    String zipCode) {
        requireNotNull(customerId, "customerId");
        requireNotBlank(street, "street");
        requireNotBlank(number, "number");
        requireNotBlank(neighborhood, "neighborhood");
        requireNotBlank(city, "city");
        requireNotBlank(state, "state");
        requireNotBlank(zipCode, "zipCode");
        return new Address(null, customerId, street, number, complement, neighborhood, city, state, zipCode, null, null);
    }

    public static Address restore(UUID id,
                                  UUID customerId,
                                  String street,
                                  String number,
                                  String complement,
                                  String neighborhood,
                                  String city,
                                  String state,
                                  String zipCode,
                                  Instant createdAt,
                                  Instant updatedAt) {
        return new Address(id, customerId, street, number, complement, neighborhood, city, state, zipCode, createdAt, updatedAt);
    }

    public void update(String street,
                       String number,
                       String complement,
                       String neighborhood,
                       String city,
                       String state,
                       String zipCode) {
        requireNotBlank(street, "street");
        requireNotBlank(number, "number");
        requireNotBlank(neighborhood, "neighborhood");
        requireNotBlank(city, "city");
        requireNotBlank(state, "state");
        requireNotBlank(zipCode, "zipCode");
        this.street = street;
        this.number = number;
        this.complement = complement;
        this.neighborhood = neighborhood;
        this.city = city;
        this.state = state;
        this.zipCode = zipCode;
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

    private static void requireNotNull(Object value, String field) {
        if (value == null) {
            throw new IllegalArgumentException(field + " must not be null");
        }
    }

}
