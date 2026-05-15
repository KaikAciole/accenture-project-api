package br.com.accenture.order.infrastructure.config;

import br.com.accenture.order.infrastructure.security.filter.InternalTrafficFilter;
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
        registrationBean.addUrlPatterns("/internal/*");

        return registrationBean;
    }
}