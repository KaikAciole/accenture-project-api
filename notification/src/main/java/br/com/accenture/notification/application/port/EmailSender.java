package br.com.accenture.notification.application.port;

public interface EmailSender {

    void sendWelcomeEmail(String recipient);
}
