package br.com.accenture.auth.domain.service;

import br.com.accenture.auth.domain.model.UserCredential;

public interface TokenProvider {
    String generateAccessToken(UserCredential userCredential);
}