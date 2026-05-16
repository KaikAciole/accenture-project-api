package br.com.accenture.auth.domain.repository;

import br.com.accenture.auth.domain.model.PasswordResetToken;

import java.util.List;
import java.util.UUID;

public interface PasswordResetTokenRepository {
    void save(PasswordResetToken token);
    List<PasswordResetToken> findUsableByCustomerId(UUID customerId);
}
