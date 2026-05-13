package br.com.accenture.assistant.api.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Evento de erro emitido durante o streaming do assistente")
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorEvent(

        @Schema(description = "Identificador curto do tipo de erro",
                example = "Rate limit exceeded")
        String title,

        @Schema(description = "Mensagem amigável para o usuário",
                example = "Assistente temporariamente indisponível por excesso de uso.")
        String detail,

        @Schema(description = "Quando reentar a requisição, em segundos. Presente apenas em rate limit.",
                example = "18")
        Long retryAfterSeconds

) {
}
