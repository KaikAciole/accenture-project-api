package br.com.accenture.notification.infrastructure.persistence.mapper;

import br.com.accenture.notification.domain.model.Notification;
import br.com.accenture.notification.infrastructure.persistence.entity.NotificationJpaEntity;

public class NotificationPersistenceMapper {

    public NotificationJpaEntity toEntity(Notification notification) {
        if (notification == null) {
            return null;
        }
        return NotificationJpaEntity.builder()
                .id(notification.getId())
                .customerId(notification.getCustomerId())
                .recipient(notification.getRecipient())
                .subject(notification.getSubject())
                .body(notification.getBody())
                .createdAt(notification.getCreatedAt())
                .sentAt(notification.getSentAt())
                .status(notification.getStatus())
                .build();
    }

    public Notification toDomain(NotificationJpaEntity entity) {
        if (entity == null) {
            return null;
        }
        return Notification.restore(
                entity.getId(),
                entity.getCustomerId(),
                entity.getRecipient(),
                entity.getSubject(),
                entity.getBody(),
                entity.getCreatedAt(),
                entity.getSentAt(),
                entity.getStatus()
        );
    }
}
