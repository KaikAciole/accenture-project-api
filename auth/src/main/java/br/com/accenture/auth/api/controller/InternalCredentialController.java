package br.com.accenture.auth.api.controller;

import br.com.accenture.auth.api.dto.request.AdminRegisterRequest;
import br.com.accenture.auth.api.dto.request.ChangeEmailRequest;
import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.command.RegisterCommand;
import br.com.accenture.auth.application.usecase.ChangeEmailUseCase;
import br.com.accenture.auth.application.usecase.RegisterUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/internal/auth/credentials")
@RequiredArgsConstructor
public class InternalCredentialController {

    private final ChangeEmailUseCase changeEmailUseCase;
    private final RegisterUserUseCase registerUserUseCase;

    @PatchMapping("/email")
    public ResponseEntity<Void> changeEmail(@RequestBody @Valid ChangeEmailRequest request) {

        var command = new ChangeEmailCommand(
                request.customerId(),
                request.newEmail()
        );

        changeEmailUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }

    @PostMapping("/admin-register")
    public ResponseEntity<Void> adminRegister(@RequestBody @Valid AdminRegisterRequest request) {

        var command = new RegisterCommand(
                request.customerId(),
                request.email(),
                request.password(),
                Set.of("ADMIN")
        );

        registerUserUseCase.execute(command);

        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
