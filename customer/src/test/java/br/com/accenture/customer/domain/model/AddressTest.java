package br.com.accenture.customer.domain.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AddressTest {

    private static final UUID CUSTOMER_ID = UUID.randomUUID();

    @Test
    void createNew_shouldBuildAddressWithoutIdAndTimestamps() {
        Address address = Address.createNew(
                CUSTOMER_ID, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000"
        );

        assertThat(address.getId()).isNull();
        assertThat(address.getCreatedAt()).isNull();
        assertThat(address.getUpdatedAt()).isNull();
        assertThat(address.getCustomerId()).isEqualTo(CUSTOMER_ID);
        assertThat(address.getStreet()).isEqualTo("Rua A");
        assertThat(address.getNumber()).isEqualTo("100");
        assertThat(address.getComplement()).isEqualTo("Apt 1");
        assertThat(address.getNeighborhood()).isEqualTo("Centro");
        assertThat(address.getCity()).isEqualTo("São Paulo");
        assertThat(address.getState()).isEqualTo("SP");
        assertThat(address.getZipCode()).isEqualTo("01001000");
    }

    @Test
    void createNew_shouldAllowNullComplement() {
        Address address = Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );
        assertThat(address.getComplement()).isNull();
    }

    @Test
    void createNew_shouldRejectNullCustomerId() {
        assertThatThrownBy(() -> Address.createNew(
                null, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("customerId");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankStreet(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, value, "100", null, "Centro", "São Paulo", "SP", "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("street");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankNumber(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, "Rua A", value, null, "Centro", "São Paulo", "SP", "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("number");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankNeighborhood(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, value, "São Paulo", "SP", "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("neighborhood");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankCity(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", value, "SP", "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("city");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankState(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", value, "01001000"
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("state");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void createNew_shouldRejectBlankZipCode(String value) {
        assertThatThrownBy(() -> Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", "SP", value
        )).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("zipCode");
    }

    @Test
    void restore_shouldRehydrateAllFields() {
        UUID id = UUID.randomUUID();
        Instant created = Instant.parse("2024-01-01T10:00:00Z");
        Instant updated = Instant.parse("2024-01-02T10:00:00Z");

        Address address = Address.restore(
                id, CUSTOMER_ID, "Rua A", "100", "Apt 1", "Centro", "São Paulo", "SP", "01001000", created, updated
        );

        assertThat(address.getId()).isEqualTo(id);
        assertThat(address.getCreatedAt()).isEqualTo(created);
        assertThat(address.getUpdatedAt()).isEqualTo(updated);
    }

    @Test
    void update_shouldChangeAllMutableFields() {
        Address address = Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );

        address.update("Rua B", "200", "Casa 2", "Bairro Novo", "Rio", "RJ", "22222222");

        assertThat(address.getStreet()).isEqualTo("Rua B");
        assertThat(address.getNumber()).isEqualTo("200");
        assertThat(address.getComplement()).isEqualTo("Casa 2");
        assertThat(address.getNeighborhood()).isEqualTo("Bairro Novo");
        assertThat(address.getCity()).isEqualTo("Rio");
        assertThat(address.getState()).isEqualTo("RJ");
        assertThat(address.getZipCode()).isEqualTo("22222222");
        assertThat(address.getCustomerId()).isEqualTo(CUSTOMER_ID);
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankStreet(String value) {
        Address address = Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );
        assertThatThrownBy(() -> address.update(value, "200", null, "Centro", "São Paulo", "SP", "01001000"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("street");
    }

    @ParameterizedTest
    @NullAndEmptySource
    void update_shouldRejectBlankZipCode(String value) {
        Address address = Address.createNew(
                CUSTOMER_ID, "Rua A", "100", null, "Centro", "São Paulo", "SP", "01001000"
        );
        assertThatThrownBy(() -> address.update("Rua B", "200", null, "Centro", "São Paulo", "SP", value))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("zipCode");
    }

}
