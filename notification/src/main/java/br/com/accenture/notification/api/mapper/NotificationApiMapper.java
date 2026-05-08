package br.com.accenture.notification.api.mapper;

import br.com.accenture.notification.api.dto.NotificationResponse;
import br.com.accenture.notification.domain.model.Notification;

public class NotificationApiMapper {

    public NotificationResponse toResponse(Notification notification) {
        return new NotificationResponse(
                notification.getId(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }
}
