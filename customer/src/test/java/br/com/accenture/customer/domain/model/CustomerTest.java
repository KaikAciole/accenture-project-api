package br.com.accenture.customer.domain.model;

import br.com.accenture.customer.domain.exception.ImmutableFieldException;
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
    void create_shouldBuildCustomerWithAllFields() {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");

        assertThat(customer.getId()).isNull();
        assertThat(customer.getCreatedAt()).isNull();
        assertThat(customer.getUpdatedAt()).isNull();
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void create_shouldRejectBlankName(String value) {
        assertThatThrownBy(() -> Customer.create(value, "maria@example.com", "12345678901", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void create_shouldRejectBlankEmail(String value) {
        assertThatThrownBy(() -> Customer.create("Maria", value, "12345678901", "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void create_shouldRejectBlankCpf(String value) {
        assertThatThrownBy(() -> Customer.create("Maria", "maria@example.com", value, "11999998888"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void create_shouldRejectBlankPhone(String value) {
        assertThatThrownBy(() -> Customer.create("Maria", "maria@example.com", "12345678901", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void restore_shouldRehydrateAllFields() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");

        Customer customer = Customer.restore(
                id, "Maria", "maria@example.com", "12345678901", "11999998888", created, updated
        );

        assertThat(customer.getId()).isEqualTo(id);
        assertThat(customer.getCreatedAt()).isEqualTo(created);
        assertThat(customer.getUpdatedAt()).isEqualTo(updated);
        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
    }

    @Test
    void updateProfile_shouldUpdateMutableFieldsWhenProvided() {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");

        customer.updateProfile("Maria Updated", null, "11900000000");

        assertThat(customer.getName()).isEqualTo("Maria Updated");
        assertThat(customer.getPhone()).isEqualTo("11900000000");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
    }

    @Test
    void updateProfile_shouldIgnoreNullFields() {
        Customer customer = Customer.restore(
                UUID.randomUUID(), "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.now(), Instant.now()
        );

        customer.updateProfile("Maria Updated", null, null);

        assertThat(customer.getName()).isEqualTo("Maria Updated");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void updateProfile_shouldRejectBlankName(String value) {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");
        assertThatThrownBy(() -> customer.updateProfile(value, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void updateProfile_shouldRejectBlankCpf(String value) {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");
        assertThatThrownBy(() -> customer.updateProfile(null, value, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void updateProfile_shouldRejectBlankPhone(String value) {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");
        assertThatThrownBy(() -> customer.updateProfile(null, null, value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void updateProfile_shouldRejectCpfChange() {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");

        assertThatThrownBy(() -> customer.updateProfile(null, "99999999999", null))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("cpf");
    }

    @Test
    void updateProfile_shouldAllowSameCpfValue() {
        Customer customer = Customer.create("Maria", "maria@example.com", "12345678901", "11999998888");

        customer.updateProfile(null, "12345678901", null);

        assertThat(customer.getCpf()).isEqualTo("12345678901");
    }

}
