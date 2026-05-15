package br.com.accenture.auth.domain.service;

public interface PasswordEncoder {
    String encode(String password);
    boolean matches(String password, String encodedPassword);
}