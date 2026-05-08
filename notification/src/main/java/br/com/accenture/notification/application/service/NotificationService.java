package br.com.accenture.notification.application.service;

import br.com.accenture.notification.domain.exceptions.NotificationNotFoundException;
import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.domain.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class NotificationService {

    private final NotificationRepository repository;

    public NotificationService(NotificationRepository repository) {
        this.repository = repository;
    }

    public Notification create(String recipient, String subject, String body) {
        Notification notification = Notification.create(recipient, subject, body);
        return repository.save(notification);
    }

    @Transactional(readOnly = true)
    public Notification findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotificationNotFoundException(id));
    }
}
