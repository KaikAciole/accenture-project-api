package br.com.accenture.notification.application.port;

import java.util.Optional;

public interface CustomerLookup {

    Optional<String> findEmailByCustomerId(String customerId);
}
