package br.com.accenture.auth.api.controller;

import br.com.accenture.auth.api.dto.request.AdminRegisterRequest;
import br.com.accenture.auth.api.dto.request.ChangeEmailRequest;
import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.exception.CredentialNotFoundException;
import br.com.accenture.auth.application.usecase.ChangeEmailUseCase;
import br.com.accenture.auth.application.usecase.RegisterUserUseCase;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalCredentialController.class)
@AutoConfigureMockMvc(addFilters = false)
class InternalCredentialControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ChangeEmailUseCase changeEmailUseCase;
    @MockitoBean
    private RegisterUserUseCase registerUserUseCase;

    @Test
    void changeEmail_shouldReturn204OnHappyPath() throws Exception {
        ChangeEmailRequest request = new ChangeEmailRequest(UUID.randomUUID(), "new@example.com");

        mockMvc.perform(patch("/internal/auth/credentials/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNoContent());

        verify(changeEmailUseCase).execute(any(ChangeEmailCommand.class));
    }

    @Test
    void changeEmail_shouldReturn404WhenCredentialNotFound() throws Exception {
        doThrow(new CredentialNotFoundException("No credentials found"))
                .when(changeEmailUseCase).execute(any(ChangeEmailCommand.class));

        ChangeEmailRequest request = new ChangeEmailRequest(UUID.randomUUID(), "new@example.com");

        mockMvc.perform(patch("/internal/auth/credentials/email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Credential Not Found"));
    }

    @Test
    void adminRegister_shouldReturn201() throws Exception {
        AdminRegisterRequest request = new AdminRegisterRequest(
                UUID.randomUUID(), "admin@example.com", "adminPass1"
        );

        mockMvc.perform(post("/internal/auth/credentials/admin-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(registerUserUseCase).execute(any(RegisterCommand.class));
    }

    @Test
    void adminRegister_shouldReturn400WhenValidationFails() throws Exception {
        String invalidJson = "{\"customerId\": null, \"email\": \"invalid\", \"password\": \"\"}";

        mockMvc.perform(post("/internal/auth/credentials/admin-register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }
}
