package br.com.accenture.notification.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient customerRestClient(@Value("${customer-service.url}") String baseUrl,
                                         @Value("${api.security.internal.secret}") String internalSecret) {
        ClientHttpRequestInterceptor internalSecretInterceptor = (request, body, execution) -> {
            request.getHeaders().set("X-Internal-Secret", internalSecret);
            return execution.execute(request, body);
        };

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(internalSecretInterceptor)
                .build();
    }
}
