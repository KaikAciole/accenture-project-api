package br.com.accenture.auth.infrastructure.persistence.mapper;

import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.infrastructure.persistence.entity.UserCredentialEntity;
import org.springframework.stereotype.Component;

@Component
public class UserCredentialMapper {

    public UserCredentialEntity toEntity(UserCredential domain) {
        if (domain == null) return null;

        return new UserCredentialEntity(
                domain.getId(),
                domain.getCustomerId(),
                domain.getEmail().value(),
                domain.getPassword().value(),
                domain.getRoles(),
                domain.isActive(),
                domain.getCreatedAt(),
                domain.getUpdatedAt()
        );
    }

    public UserCredential toDomain(UserCredentialEntity entity) {
        if (entity == null) return null;

        return UserCredential.restore(
                entity.getId(),
                entity.getCustomerId(),
                entity.getEmail(),
                entity.getPassword(),
                entity.getRoles(),
                entity.isActive(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}