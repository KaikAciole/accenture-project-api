package br.com.accenture.auth.infrastructure.persistence;

import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.repository.UserCredentialRepository;
import br.com.accenture.auth.domain.vo.Email;
import br.com.accenture.auth.infrastructure.persistence.mapper.UserCredentialMapper;
import br.com.accenture.auth.infrastructure.persistence.SpringDataUserCredentialRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class UserCredentialRepositoryAdapter implements UserCredentialRepository {

    private final SpringDataUserCredentialRepository jpaRepository;
    private final UserCredentialMapper mapper;

    @Override
    public void save(UserCredential userCredential) {
        var entity = mapper.toEntity(userCredential);
        jpaRepository.save(entity);
    }

    @Override
    public Optional<UserCredential> findByEmail(Email email) {
        return jpaRepository.findByEmail(email.value())
                .map(mapper::toDomain);
    }

    @Override
    public boolean existsByEmail(Email email) {
        return jpaRepository.existsByEmail(email.value());
    }
}