package br.com.accenture.auth.domain.repository;

import br.com.accenture.auth.domain.model.UserCredential;
import br.com.accenture.auth.domain.vo.Email;

import java.util.Optional;
import java.util.UUID;

public interface UserCredentialRepository {
    void save(UserCredential userCredential);
    Optional<UserCredential> findByEmail(Email email);
    boolean existsByEmail(Email email);
}