package br.com.accenture.auth.domain.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class RoleTest {

    @Test
    void shouldExposeExpectedValues() {
        assertThat(Role.values()).containsExactly(Role.CUSTOMER, Role.ADMIN);
    }

    @Test
    void valueOf_shouldRoundTrip() {
        for (Role role : Role.values()) {
            assertThat(Role.valueOf(role.name())).isEqualTo(role);
        }
    }
}
