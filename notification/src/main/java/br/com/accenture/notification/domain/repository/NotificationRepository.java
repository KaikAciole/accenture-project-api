package br.com.accenture.notification.domain.repository;

import br.com.accenture.notification.domain.model.Notification;

import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository {

    Notification save(Notification notification);

    Optional<Notification> findById(UUID id);

    Optional<Notification> findFirstByCustomerId(String customerId);
}
