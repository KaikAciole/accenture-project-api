package br.com.accenture.assistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Pergunta do usuário ao assistente virtual")
public record AskRequest(

        @Schema(description = "Pergunta em linguagem natural",
                example = "Como funciona a recarga de saldo?",
                requiredMode = Schema.RequiredMode.REQUIRED)
        @NotBlank(message = "question must not be blank")
        @Size(min = 1, max = 2000, message = "question must be between 1 and 2000 characters")
        String question

) {
}
