package br.com.accenture.assistent.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Resposta gerada pelo assistente virtual")
public record AskResponse(

        @Schema(description = "Texto da resposta gerada pelo assistente",
                example = "A recarga de saldo é feita na aba \"Carteira\", clicando em \"Adicionar saldo\".")
        String answer

) {
}
