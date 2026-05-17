package br.com.accenture.auth.api.controller;

import br.com.accenture.auth.api.dto.request.ForgotPasswordRequest;
import br.com.accenture.auth.api.dto.request.LoginRequest;
import br.com.accenture.auth.api.dto.request.RegisterRequest;
import br.com.accenture.auth.api.dto.request.ResetPasswordRequest;
import br.com.accenture.auth.application.command.LoginCommand;
import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.command.RequestPasswordResetCommand;
import br.com.accenture.auth.application.command.ResetPasswordCommand;
import br.com.accenture.auth.application.exception.InvalidCredentialsException;
import br.com.accenture.auth.application.exception.UserAlreadyExistsException;
import br.com.accenture.auth.application.usecase.AuthenticateUserUseCase;
import br.com.accenture.auth.application.usecase.RegisterUserUseCase;
import br.com.accenture.auth.application.usecase.RequestPasswordResetUseCase;
import br.com.accenture.auth.application.usecase.ResetPasswordUseCase;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;
    @MockitoBean
    private AuthenticateUserUseCase authenticateUserUseCase;
    @MockitoBean
    private RequestPasswordResetUseCase requestPasswordResetUseCase;
    @MockitoBean
    private ResetPasswordUseCase resetPasswordUseCase;

    @Test
    void register_shouldReturn201OnHappyPath() throws Exception {
        RegisterRequest request = new RegisterRequest(
                UUID.randomUUID(), "user@example.com", "rawPass1", Set.of("CUSTOMER")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(registerUserUseCase).execute(any(RegisterCommand.class));
    }

    @Test
    void register_shouldReturn400WhenValidationFails() throws Exception {
        String invalidJson = "{\"email\": \"not-an-email\", \"password\": \"\", \"roles\": []}";

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }

    @Test
    void register_shouldReturn409WhenUserAlreadyExists() throws Exception {
        doThrow(new UserAlreadyExistsException("User already exists with this email."))
                .when(registerUserUseCase).execute(any(RegisterCommand.class));

        RegisterRequest request = new RegisterRequest(
                UUID.randomUUID(), "dup@example.com", "rawPass1", Set.of("CUSTOMER")
        );

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("User Already Exists"));
    }

    @Test
    void login_shouldReturnTokenOnHappyPath() throws Exception {
        when(authenticateUserUseCase.execute(any(LoginCommand.class))).thenReturn("jwt-token");

        LoginRequest request = new LoginRequest("user@example.com", "rawPass1");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("jwt-token"));
    }

    @Test
    void login_shouldReturn401WhenCredentialsInvalid() throws Exception {
        when(authenticateUserUseCase.execute(any(LoginCommand.class)))
                .thenThrow(new InvalidCredentialsException("Invalid email or password."));

        LoginRequest request = new LoginRequest("user@example.com", "wrong");

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("Unauthorized"));
    }

    @Test
    void forgotPassword_shouldReturn204() throws Exception {
        ForgotPasswordRequest request = new ForgotPasswordRequest("user@example.com");

        mockMvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(requestPasswordResetUseCase).execute(any(RequestPasswordResetCommand.class));
    }

    @Test
    void resetPassword_shouldReturn204() throws Exception {
        ResetPasswordRequest request = new ResetPasswordRequest("user@example.com", "tok", "newPass123");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(resetPasswordUseCase).execute(any(ResetPasswordCommand.class));
    }
}
