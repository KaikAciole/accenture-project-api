package br.com.accenture.notification.api.controller;

import br.com.accenture.notification.application.service.NotificationService;
import br.com.accenture.notification.domain.exceptions.NotificationNotFoundException;
import br.com.accenture.notification.support.TestFixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(NotificationController.class)
class NotificationControllerIntegrationTest {

    private static final String INTERNAL_SECRET_HEADER = "X-Internal-Secret";
    private static final String INTERNAL_SECRET_VALUE = "senha-secreta-microsservicos-1234";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private NotificationService service;

    @Test
    void getReturnsOkAndPayloadWhenNotificationExists() throws Exception {
        when(service.findById(TestFixtures.NOTIFICATION_ID))
                .thenReturn(TestFixtures.restoredSentNotification());

        mockMvc.perform(get("/notifications/{id}", TestFixtures.NOTIFICATION_ID)
                        .header(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(TestFixtures.NOTIFICATION_ID.toString()))
                .andExpect(jsonPath("$.customerId").value(TestFixtures.CUSTOMER_ID))
                .andExpect(jsonPath("$.recipient").value(TestFixtures.RECIPIENT))
                .andExpect(jsonPath("$.subject").value(TestFixtures.SUBJECT))
                .andExpect(jsonPath("$.body").value(TestFixtures.BODY))
                .andExpect(jsonPath("$.status").value("SENT"))
                .andExpect(jsonPath("$.createdAt").exists())
                .andExpect(jsonPath("$.sentAt").exists());
    }

    @Test
    void getReturnsNotFoundWhenServiceThrows() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.findById(id)).thenThrow(new NotificationNotFoundException(id));

        mockMvc.perform(get("/notifications/{id}", id)
                        .header(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Notification not found: " + id));
    }

    @Test
    void getReturnsBadRequestWhenIdIsNotAValidUuid() throws Exception {
        mockMvc.perform(get("/notifications/{id}", "not-a-uuid")
                        .header(INTERNAL_SECRET_HEADER, INTERNAL_SECRET_VALUE))
                .andExpect(status().isBadRequest());
    }
}
