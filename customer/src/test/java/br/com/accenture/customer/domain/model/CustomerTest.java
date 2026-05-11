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
    void createMinimal_shouldBuildCustomerWithOnlyEmail() {
        Customer customer = Customer.createMinimal("maria@example.com");

        assertThat(customer.getId()).isNull();
        assertThat(customer.getCreatedAt()).isNull();
        assertThat(customer.getUpdatedAt()).isNull();
        assertThat(customer.getEmail()).isEqualTo("maria@example.com");
        assertThat(customer.getName()).isNull();
        assertThat(customer.getCpf()).isNull();
        assertThat(customer.getPhone()).isNull();
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "\t"})
    void createMinimal_shouldRejectBlankEmail(String value) {
        assertThatThrownBy(() -> Customer.createMinimal(value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("email");
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
    void restore_shouldAllowNullableProfileFields() {
        Customer customer = Customer.restore(
                UUID.randomUUID(), null, "maria@example.com", null, null, Instant.now(), Instant.now()
        );

        assertThat(customer.getName()).isNull();
        assertThat(customer.getCpf()).isNull();
        assertThat(customer.getPhone()).isNull();
    }

    @Test
    void updateProfile_shouldUpdateAllFieldsWhenAllProvided() {
        Customer customer = Customer.createMinimal("maria@example.com");

        customer.updateProfile("Maria", "12345678901", "11999998888");

        assertThat(customer.getName()).isEqualTo("Maria");
        assertThat(customer.getCpf()).isEqualTo("12345678901");
        assertThat(customer.getPhone()).isEqualTo("11999998888");
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
        Customer customer = Customer.createMinimal("maria@example.com");
        assertThatThrownBy(() -> customer.updateProfile(value, null, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("name");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void updateProfile_shouldRejectBlankCpf(String value) {
        Customer customer = Customer.createMinimal("maria@example.com");
        assertThatThrownBy(() -> customer.updateProfile(null, value, null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cpf");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t"})
    void updateProfile_shouldRejectBlankPhone(String value) {
        Customer customer = Customer.createMinimal("maria@example.com");
        assertThatThrownBy(() -> customer.updateProfile(null, null, value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("phone");
    }

    @Test
    void updateProfile_shouldAllowFirstCpfDefinition() {
        Customer customer = Customer.createMinimal("maria@example.com");

        customer.updateProfile(null, "12345678901", null);

        assertThat(customer.getCpf()).isEqualTo("12345678901");
    }

    @Test
    void updateProfile_shouldRejectCpfChangeAfterDefinition() {
        Customer customer = Customer.restore(
                UUID.randomUUID(), "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.now(), Instant.now()
        );

        assertThatThrownBy(() -> customer.updateProfile(null, "99999999999", null))
                .isInstanceOf(ImmutableFieldException.class)
                .hasMessageContaining("cpf");
    }

    @Test
    void updateProfile_shouldAllowSameCpfValue() {
        Customer customer = Customer.restore(
                UUID.randomUUID(), "Maria", "maria@example.com", "12345678901", "11999998888",
                Instant.now(), Instant.now()
        );

        customer.updateProfile(null, "12345678901", null);

        assertThat(customer.getCpf()).isEqualTo("12345678901");
    }

}
