package br.com.accenture.api_gateway.dto;

import java.util.List;

public record SeedDataResponse(
        String adminEmail,
        boolean adminCreated,
        int customersCreated,
        int customersSkipped,
        int productsCreated,
        int productsSkipped,
        int walletsCredited,
        String seedOrderId,
        String seedOrderStatus,
        List<String> warnings
) {}
