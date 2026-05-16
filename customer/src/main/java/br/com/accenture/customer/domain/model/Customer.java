package br.com.accenture.customer.domain.model;

import br.com.accenture.customer.domain.exception.ImmutableFieldException;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Customer {

    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;

    private Customer(UUID id,
                     String name,
                     String email,
                     String cpf,
                     String phone,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Customer create(String name, String email, String cpf, String phone) {
        requireNotBlank(name, "name");
        requireNotBlank(email, "email");
        requireNotBlank(cpf, "cpf");
        requireNotBlank(phone, "phone");
        return new Customer(null, name, email, cpf, phone, null, null);
    }

    public static Customer restore(UUID id,
                                   String name,
                                   String email,
                                   String cpf,
                                   String phone,
                                   Instant createdAt,
                                   Instant updatedAt) {
        return new Customer(id, name, email, cpf, phone, createdAt, updatedAt);
    }

    public void updateProfile(String name, String email, String cpf, String phone) {
        if (name != null) {
            requireNotBlank(name, "name");
            this.name = name;
        }
        if (email != null) {
            requireNotBlank(email, "email");
            this.email = email;
        }
        if (cpf != null) {
            requireNotBlank(cpf, "cpf");
            if (this.cpf != null && !this.cpf.equals(cpf)) {
                throw new ImmutableFieldException("cpf");
            }
            this.cpf = cpf;
        }
        if (phone != null) {
            requireNotBlank(phone, "phone");
            this.phone = phone;
        }
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

}
