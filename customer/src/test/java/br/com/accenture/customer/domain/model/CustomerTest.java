package br.com.accenture.customer.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CustomerTest {

    @Test
    void createNew_shouldBuildCustomerWithoutIdAndTimestamps() {
        Customer customer = Customer.createNew(
                "Maria",
                "maria@example.com",
                "12345678901",
                "secret123",
                "11999998888"
        );

        assertThat(customer.getId()).isNull();
        assertThat(customer.getCreatedAt()).isNull();
        assertThat(customer.getUpdatedAt()).isNull();
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPassword()).isEqualTo("secret123");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void createNew_shouldRejectBlankName(String value) {
        assertThatThrownBy(() -> Customer.createNew(value, "a@b.com", "12345678901", "secret123", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"  "})
    void createNew_shouldRejectBlankEmail(String value) {
        assertThatThrownBy(() -> Customer.createNew("Maria", value, "12345678901", "secret123", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankCpf(String value) {
        assertThatThrownBy(() -> Customer.createNew("Maria", "a@b.com", value, "secret123", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankPassword(String value) {
        assertThatThrownBy(() -> Customer.createNew("Maria", "a@b.com", "12345678901", value, "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankPhone(String value) {
        assertThatThrownBy(() -> Customer.createNew("Maria", "a@b.com", "12345678901", "secret123", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void restore_shouldRehydrateAllFields() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");

        Customer customer = Customer.restore(
                id, "Maria", "maria@example.com", "12345678901", "secret123", "11999998888", created, updated
        );

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getCreatedAt()).isEqualTo(created);
        assertThat(customer.getUpdatedAt()).isEqualTo(updated);
        assertThat(customer.getName()).isEqualTo("Maria");
    }

    @Test
    void restore_shouldNotValidateBlankFields() {
        Customer customer = Customer.restore(
                UUID.randomUUID(), "", "", "", "", "", Instant.now(), Instant.now()
        );
        assertThat(customer.getName()).isEmpty();
    }

    @Test
    void update_shouldChangeMutableFields() {
        Customer customer = Customer.createNew("Maria", "old@example.com", "12345678901", "secret123", "11999998888");

        customer.update("Maria Updated", "new@example.com", "newpass12", "11988887777");

        assertThat(customer.getName()).isEqualTo("Maria Updated");
        assertThat(customer.getEmail()).isEqualTo("new@example.com");
        assertThat(customer.getPassword()).isEqualTo("newpass12");
        assertThat(customer.getPhone()).isEqualTo("11988887777");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankName(String value) {
        Customer customer = Customer.createNew("Maria", "a@b.com", "12345678901", "secret123", "11999998888");
        assertThatThrownBy(() -> customer.update(value, "a@b.com", "secret123", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankEmail(String value) {
        Customer customer = Customer.createNew("Maria", "a@b.com", "12345678901", "secret123", "11999998888");
        assertThatThrownBy(() -> customer.update("Maria", value, "secret123", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankPassword(String value) {
        Customer customer = Customer.createNew("Maria", "a@b.com", "12345678901", "secret123", "11999998888");
        assertThatThrownBy(() -> customer.update("Maria", "a@b.com", value, "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("password");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankPhone(String value) {
        Customer customer = Customer.createNew("Maria", "a@b.com", "12345678901", "secret123", "11999998888");
        assertThatThrownBy(() -> customer.update("Maria", "a@b.com", "secret123", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

}
