package br.com.accenture.auth.api.controller;

import br.com.accenture.auth.api.dto.request.ForgotPasswordRequest;
import br.com.accenture.auth.api.dto.request.LoginRequest;
import br.com.accenture.auth.api.dto.request.RegisterRequest;
import br.com.accenture.auth.api.dto.request.ResetPasswordRequest;
import br.com.accenture.auth.api.dto.response.TokenResponse;
import br.com.accenture.auth.application.command.LoginCommand;
import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.command.RequestPasswordResetCommand;
import br.com.accenture.auth.application.command.ResetPasswordCommand;
import br.com.accenture.auth.application.usecase.AuthenticateUserUseCase;
import br.com.accenture.auth.application.usecase.RegisterUserUseCase;
import br.com.accenture.auth.application.usecase.RequestPasswordResetUseCase;
import br.com.accenture.auth.application.usecase.ResetPasswordUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final RegisterUserUseCase registerUserUseCase;
    private final AuthenticateUserUseCase authenticateUserUseCase;
    private final RequestPasswordResetUseCase requestPasswordResetUseCase;
    private final ResetPasswordUseCase resetPasswordUseCase;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterRequest request) {

        var command = new RegisterCommand(
                request.customerId(),
                request.email(),
                request.password(),
                request.roles()
        );

        registerUserUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@RequestBody @Valid LoginRequest request) {

        var command = new LoginCommand(
                request.email(),
                request.password()
        );

        String token = authenticateUserUseCase.execute(command);

        return ResponseEntity.ok(new TokenResponse(token));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {

        requestPasswordResetUseCase.execute(new RequestPasswordResetCommand(request.email()));

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {

        resetPasswordUseCase.execute(new ResetPasswordCommand(
                request.email(),
                request.token(),
                request.newPassword()
        ));

        return ResponseEntity.noContent().build();
    }
}