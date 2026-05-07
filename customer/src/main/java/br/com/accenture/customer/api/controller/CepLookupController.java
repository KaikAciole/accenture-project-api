package br.com.accenture.customer.api.controller;

import br.com.accenture.customer.api.dto.CepLookupRequest;
import br.com.accenture.customer.api.dto.CepLookupResponse;
import br.com.accenture.customer.api.mapper.CepLookupMapper;
import br.com.accenture.customer.application.service.CepLookupService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cep")
public class CepLookupController {

    private final CepLookupService cepLookupService;

    public CepLookupController(CepLookupService cepLookupService) {
        this.cepLookupService = cepLookupService;
    }

    @PostMapping("/lookup")
    public CepLookupResponse lookup(@Valid @RequestBody CepLookupRequest request) {
        return CepLookupMapper.toResponse(cepLookupService.lookup(request.cep()));
    }

}
