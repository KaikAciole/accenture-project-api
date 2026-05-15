package br.com.accenture.auth.api.controller;

import br.com.accenture.auth.api.dto.request.ChangeEmailRequest;
import br.com.accenture.auth.application.command.ChangeEmailCommand;
import br.com.accenture.auth.application.usecase.ChangeEmailUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/auth/credentials")
@RequiredArgsConstructor
public class InternalCredentialController {

    private final ChangeEmailUseCase changeEmailUseCase;

    @PatchMapping("/email")
    public ResponseEntity<Void> changeEmail(@RequestBody @Valid ChangeEmailRequest request) {

        var command = new ChangeEmailCommand(
                request.customerId(),
                request.newEmail()
        );

        changeEmailUseCase.execute(command);

        return ResponseEntity.noContent().build();
    }
}
