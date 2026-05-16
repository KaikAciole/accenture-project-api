package br.com.accenture.api_gateway.controller;

import br.com.accenture.api_gateway.dto.SeedDataResponse;
import br.com.accenture.api_gateway.service.SeedDataService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/gateway")
public class SeedDataController {

    private final SeedDataService seedDataService;

    public SeedDataController(SeedDataService seedDataService) {
        this.seedDataService = seedDataService;
    }

    @PostMapping("/seed-data")
    public ResponseEntity<SeedDataResponse> seedData() {
        log.info("Iniciando seed manual de dados");
        SeedDataResponse response = seedDataService.run();
        log.info("Seed concluido: {}", response);
        return ResponseEntity.ok(response);
    }
}
