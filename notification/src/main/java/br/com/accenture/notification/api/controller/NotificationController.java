package br.com.accenture.notification.api.controller;

import br.com.accenture.notification.api.dto.NotificationResponse;
import br.com.accenture.notification.api.mapper.NotificationApiMapper;
import br.com.accenture.notification.application.service.NotificationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/notifications")
public class NotificationController {

    private final NotificationService service;
    private final NotificationApiMapper mapper = new NotificationApiMapper();

    public NotificationController(NotificationService service) {
        this.service = service;
    }

    @GetMapping("/{id}")
    public NotificationResponse findById(@PathVariable UUID id) {
        return mapper.toResponse(service.findById(id));
    }
}
