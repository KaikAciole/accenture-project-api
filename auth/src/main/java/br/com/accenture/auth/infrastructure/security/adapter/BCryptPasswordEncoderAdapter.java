package br.com.accenture.auth.infrastructure.security.adapter;

import br.com.accenture.auth.domain.service.PasswordEncoder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class BCryptPasswordEncoderAdapter implements PasswordEncoder {

    private final org.springframework.security.crypto.password.PasswordEncoder springPasswordEncoder;

    @Override
    public String encode(String password) {
        return springPasswordEncoder.encode(password);
    }

    @Override
    public boolean matches(String password, String encodedPassword) {
        return springPasswordEncoder.matches(password, encodedPassword);
    }
}