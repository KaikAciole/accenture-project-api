package br.com.accenture.notification.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient customerRestClient(@Value("${customer-service.url}") String baseUrl,
                                         @Value("${api.security.internal.secret}") String internalSecret) {
        ClientHttpRequestInterceptor internalSecretInterceptor = (request, body, execution) -> {
            request.getHeaders().set("X-Internal-Secret", internalSecret);
            return execution.execute(request, body);
        };

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestInterceptor(internalSecretInterceptor)
                .build();
    }
}
