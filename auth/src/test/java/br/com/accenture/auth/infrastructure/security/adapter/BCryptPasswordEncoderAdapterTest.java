package br.com.accenture.auth.infrastructure.security.adapter;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BCryptPasswordEncoderAdapterTest {

    @Mock
    private PasswordEncoder springPasswordEncoder;

    @InjectMocks
    private BCryptPasswordEncoderAdapter adapter;

    @Test
    void encode_shouldDelegateToSpringPasswordEncoder() {
        when(springPasswordEncoder.encode("raw")).thenReturn("hashed");

        assertThat(adapter.encode("raw")).isEqualTo("hashed");
    }

    @Test
    void matches_shouldDelegateToSpringPasswordEncoder() {
        when(springPasswordEncoder.matches("raw", "hashed")).thenReturn(true);

        assertThat(adapter.matches("raw", "hashed")).isTrue();
    }
}
