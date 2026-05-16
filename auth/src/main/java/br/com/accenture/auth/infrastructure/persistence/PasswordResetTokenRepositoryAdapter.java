package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.domain.model.PasswordResetToken;
import br.com.accenture.auth.domain.repository.PasswordResetTokenRepository;
import br.com.accenture.auth.infrastructure.persistence.entity.PasswordResetTokenEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final SpringDataPasswordResetTokenRepository jpaRepository;

    @Override
    public void save(PasswordResetToken token) {
        PasswordResetTokenEntity entity = new PasswordResetTokenEntity(
                token.getId(),
                token.getCustomerId(),
                token.getTokenHash(),
                token.getExpiresAt(),
                token.getCreatedAt(),
                token.getUsedAt()
        );
        jpaRepository.save(entity);
    }

    @Override
    public List<PasswordResetToken> findUsableByCustomerId(UUID customerId) {
        return jpaRepository.findAllByCustomerIdAndUsedAtIsNullAndExpiresAtAfter(customerId, Instant.now())
                .stream()
                .map(e -> PasswordResetToken.restore(
                        e.getId(),
                        e.getCustomerId(),
                        e.getTokenHash(),
                        e.getExpiresAt(),
                        e.getCreatedAt(),
                        e.getUsedAt()
                ))
                .toList();
    }
}
