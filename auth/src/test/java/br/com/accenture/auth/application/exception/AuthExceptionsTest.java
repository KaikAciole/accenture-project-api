package br.com.accenture.auth.application.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuthExceptionsTest {

    @Test
    void invalidCredentialsException_shouldStoreMessage() {
        InvalidCredentialsException ex = new InvalidCredentialsException("bad");
        assertThat(ex.getMessage()).isEqualTo("bad");
    }

    @Test
    void userAlreadyExistsException_shouldStoreMessage() {
        UserAlreadyExistsException ex = new UserAlreadyExistsException("dup");
        assertThat(ex.getMessage()).isEqualTo("dup");
    }

    @Test
    void credentialNotFoundException_shouldStoreMessage() {
        CredentialNotFoundException ex = new CredentialNotFoundException("missing");
        assertThat(ex.getMessage()).isEqualTo("missing");
    }

    @Test
    void invalidPasswordResetTokenException_shouldStoreMessage() {
        InvalidPasswordResetTokenException ex = new InvalidPasswordResetTokenException("bad token");
        assertThat(ex.getMessage()).isEqualTo("bad token");
    }
}
