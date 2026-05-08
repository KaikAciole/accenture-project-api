package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CepLookupRequest;
import br.com.accenture.customer.api.dto.CepLookupResponse;
import br.com.accenture.customer.api.mapper.CepLookupMapper;
import br.com.accenture.customer.application.service.CepLookupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cep")
@Tag(name = "CEP", description = "Consulta de endereço por CEP via integração com ViaCEP")
public class CepLookupController {

    private final CepLookupService cepLookupService;

    public CepLookupController(CepLookupService cepLookupService) {
        this.cepLookupService = cepLookupService;
    }

    @PostMapping("/lookup")
    @Operation(
            summary = "Consulta um CEP no ViaCEP",
            description = "Recebe um CEP de 8 dígitos e retorna os dados de endereço retornados pela integração ViaCEP."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "CEP encontrado",
                    content = @Content(schema = @Schema(implementation = CepLookupResponse.class))),
            @ApiResponse(responseCode = "400", description = "Falha de validação no payload (CEP em formato inválido)",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "CEP não encontrado no ViaCEP",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "502", description = "Falha ao se comunicar com o ViaCEP",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "Erro interno inesperado",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CepLookupResponse lookup(@Valid @RequestBody CepLookupRequest request) {
        return CepLookupMapper.toResponse(cepLookupService.lookup(request.cep()));
    }

}
