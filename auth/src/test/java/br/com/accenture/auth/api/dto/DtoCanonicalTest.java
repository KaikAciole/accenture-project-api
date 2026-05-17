package br.com.accenture.auth.api.dto;

import br.com.accenture.auth.api.dto.request.AdminRegisterRequest;
import br.com.accenture.auth.api.dto.request.ChangeEmailRequest;
import br.com.accenture.auth.api.dto.request.ForgotPasswordRequest;
import br.com.accenture.auth.api.dto.request.LoginRequest;
import br.com.accenture.auth.api.dto.request.RegisterRequest;
import br.com.accenture.auth.api.dto.request.ResetPasswordRequest;
import br.com.accenture.auth.api.dto.response.TokenResponse;
import br.com.accenture.auth.application.command.LoginCommand;
import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.command.RequestPasswordResetCommand;
import br.com.accenture.auth.application.command.ResetPasswordCommand;
import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.event.PasswordResetRequestedEvent;
import br.com.accenture.auth.application.event.UserRegisteredEvent;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class DtoCanonicalTest {

    @Test
    void requestRecords_shouldExposeAllAccessors() {
        UUID customerId = UUID.randomUUID();

        RegisterRequest register = new RegisterRequest(customerId, "u@e.com", "pw", Set.of("CUSTOMER"));
        assertThat(register.customerId()).isEqualTo(customerId);
        assertThat(register.email()).isEqualTo("u@e.com");
        assertThat(register.password()).isEqualTo("pw");
        assertThat(register.roles()).containsExactly("CUSTOMER");

        LoginRequest login = new LoginRequest("u@e.com", "pw");
        assertThat(login.email()).isEqualTo("u@e.com");
        assertThat(login.password()).isEqualTo("pw");

        ForgotPasswordRequest forgot = new ForgotPasswordRequest("u@e.com");
        assertThat(forgot.email()).isEqualTo("u@e.com");

        ResetPasswordRequest reset = new ResetPasswordRequest("u@e.com", "tok", "newPass");
        assertThat(reset.email()).isEqualTo("u@e.com");
        assertThat(reset.token()).isEqualTo("tok");
        assertThat(reset.newPassword()).isEqualTo("newPass");

        AdminRegisterRequest admin = new AdminRegisterRequest(customerId, "a@e.com", "pw1234");
        assertThat(admin.customerId()).isEqualTo(customerId);
        assertThat(admin.email()).isEqualTo("a@e.com");
        assertThat(admin.password()).isEqualTo("pw1234");

        ChangeEmailRequest change = new ChangeEmailRequest(customerId, "n@e.com");
        assertThat(change.customerId()).isEqualTo(customerId);
        assertThat(change.newEmail()).isEqualTo("n@e.com");
    }

    @Test
    void tokenResponse_shouldExposeAccessToken() {
        TokenResponse response = new TokenResponse("jwt");
        assertThat(response.accessToken()).isEqualTo("jwt");
    }

    @Test
    void commands_shouldExposeAllAccessors() {
        UUID customerId = UUID.randomUUID();

        LoginCommand login = new LoginCommand("u@e.com", "pw");
        assertThat(login.email()).isEqualTo("u@e.com");
        assertThat(login.password()).isEqualTo("pw");

        RegisterCommand register = new RegisterCommand(customerId, "u@e.com", "pw", Set.of("CUSTOMER"));
        assertThat(register.customerId()).isEqualTo(customerId);

        RequestPasswordResetCommand request = new RequestPasswordResetCommand("u@e.com");
        assertThat(request.email()).isEqualTo("u@e.com");

        ResetPasswordCommand resetCmd = new ResetPasswordCommand("u@e.com", "tok", "newPw");
        assertThat(resetCmd.email()).isEqualTo("u@e.com");
        assertThat(resetCmd.token()).isEqualTo("tok");
        assertThat(resetCmd.newPassword()).isEqualTo("newPw");

        ChangeEmailCommand changeCmd = new ChangeEmailCommand(customerId, "n@e.com");
        assertThat(changeCmd.customerId()).isEqualTo(customerId);
        assertThat(changeCmd.newEmail()).isEqualTo("n@e.com");
    }

    @Test
    void events_shouldExposeAllAccessors() {
        UUID customerId = UUID.randomUUID();

        UserRegisteredEvent userEvent = new UserRegisteredEvent(customerId, "u@e.com");
        assertThat(userEvent.customerId()).isEqualTo(customerId);
        assertThat(userEvent.email()).isEqualTo("u@e.com");

        PasswordResetRequestedEvent resetEvent = new PasswordResetRequestedEvent(customerId, "u@e.com", "tok");
        assertThat(resetEvent.customerId()).isEqualTo(customerId);
        assertThat(resetEvent.email()).isEqualTo("u@e.com");
        assertThat(resetEvent.token()).isEqualTo("tok");
    }
}
