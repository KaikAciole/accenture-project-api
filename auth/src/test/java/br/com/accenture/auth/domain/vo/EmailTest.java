package br.com.accenture.auth.domain.vo;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class EmailTest {

    @Test
    void shouldAcceptValidEmail() {
        Email email = new Email("user@example.com");
        assertThat(email.value()).isEqualTo("user@example.com");
    }

    @Test
    void shouldAcceptEmailWithPlusAndDots() {
        Email email = new Email("first.last+tag@sub.example.co");
        assertThat(email.value()).isEqualTo("first.last+tag@sub.example.co");
    }

    @Test
    void shouldThrowWhenValueIsNull() {
        assertThatThrownBy(() -> new Email(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void shouldThrowWhenValueIsBlank() {
        assertThatThrownBy(() -> new Email("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("null or empty");
    }

    @Test
    void shouldThrowWhenFormatIsInvalid() {
        assertThatThrownBy(() -> new Email("not-an-email"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid email format");
    }

    @Test
    void recordEqualityShouldBeValueBased() {
        assertThat(new Email("a@b.com")).isEqualTo(new Email("a@b.com"));
        assertThat(new Email("a@b.com")).isNotEqualTo(new Email("c@d.com"));
    }
}
