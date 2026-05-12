package br.com.accenture.assistant.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pedaço de texto emitido pelo assistente durante o streaming")
public record ChunkEvent(

        @Schema(description = "Fragmento de texto da resposta",
                example = "Olá, ")
        String content

) {
}
