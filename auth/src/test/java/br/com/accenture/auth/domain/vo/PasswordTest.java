package br.com.accenture.auth.domain.vo;

import br.com.accenture.auth.domain.service.PasswordEncoder;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PasswordTest {

    private final PasswordEncoder encoder = new PasswordEncoder() {
        @Override public String encode(String password) { return "hashed:" + password; }
        @Override public boolean matches(String password, String encodedPassword) {
            return encodedPassword.equals("hashed:" + password);
        }
    };

    @Test
    void create_shouldEncodePasswordWhenLongEnough() {
        Password password = Password.create("longEnoughPass", encoder);

        assertThat(password.value()).isEqualTo("hashed:longEnoughPass");
    }

    @Test
    void create_shouldThrowWhenPasswordIsNull() {
        assertThatThrownBy(() -> Password.create(null, encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void create_shouldThrowWhenPasswordTooShort() {
        assertThatThrownBy(() -> Password.create("short", encoder))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8 characters");
    }

    @Test
    void restore_shouldWrapAlreadyHashedValue() {
        Password password = Password.restore("$2a$10$alreadyHashed");

        assertThat(password.value()).isEqualTo("$2a$10$alreadyHashed");
    }

    @Test
    void recordValidation_shouldRejectNullValue() {
        assertThatThrownBy(() -> new Password(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void recordValidation_shouldRejectBlankValue() {
        assertThatThrownBy(() -> new Password("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }
}
