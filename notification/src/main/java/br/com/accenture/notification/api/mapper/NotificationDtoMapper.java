package br.com.accenture.notification.api.mapper;

import br.com.accenture.notification.api.dto.NotificationResponse;
import br.com.accenture.notification.domain.model.Notification;

public final class NotificationDtoMapper {

    private NotificationDtoMapper() {
    }

    public static NotificationResponse toResponse(Notification notification) {
        if (notification == null) {
            return null;
        }
        return new NotificationResponse(
                notification.getId(),
                notification.getCustomerId(),
                notification.getRecipient(),
                notification.getSubject(),
                notification.getBody(),
                notification.getStatus(),
                notification.getCreatedAt(),
                notification.getSentAt()
        );
    }

}
