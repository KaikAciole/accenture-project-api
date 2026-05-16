package br.com.accenture.auth.infrastructure.config;

import br.com.accenture.auth.infrastructure.security.filter.InternalTrafficFilter;
import jakarta.servlet.Filter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {

    @Value("${api.security.internal.secret:senha-secreta-microsservicos-1234}")
    private String internalSecret;

    @Bean
    public FilterRegistrationBean<Filter> internalTrafficFilterRegistration() {
        FilterRegistrationBean<Filter> registrationBean = new FilterRegistrationBean<>();

        registrationBean.setFilter(new InternalTrafficFilter(internalSecret));
        registrationBean.addUrlPatterns(
                "/api/v1/auth/register",
                "/internal/auth/credentials/email",
                "/internal/auth/credentials/admin-register"
        );

        return registrationBean;
    }
}