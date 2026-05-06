package br.com.accenture.customer.domain.model;

import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class Customer {

    private UUID id;
    private String name;
    private String email;
    private String cpf;
    private String password;
    private String phone;
    private Instant createdAt;
    private Instant updatedAt;

    private Customer(UUID id,
                     String name,
                     String email,
                     String cpf,
                     String password,
                     String phone,
                     Instant createdAt,
                     Instant updatedAt) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.cpf = cpf;
        this.password = password;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Customer createNew(String name,
                                     String email,
                                     String cpf,
                                     String password,
                                     String phone) {
        requireNotBlank(name, "name");
        requireNotBlank(email, "email");
        requireNotBlank(cpf, "cpf");
        requireNotBlank(password, "password");
        requireNotBlank(phone, "phone");
        return new Customer(null, name, email, cpf, password, phone, null, null);
    }

    public static Customer restore(UUID id,
                                   String name,
                                   String email,
                                   String cpf,
                                   String password,
                                   String phone,
                                   Instant createdAt,
                                   Instant updatedAt) {
        return new Customer(id, name, email, cpf, password, phone, createdAt, updatedAt);
    }

    public void update(String name, String email, String password, String phone) {
        requireNotBlank(name, "name");
        requireNotBlank(email, "email");
        requireNotBlank(password, "password");
        requireNotBlank(phone, "phone");
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
    }

    private static void requireNotBlank(String value, String field) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(field + " must not be blank");
        }
    }

}
