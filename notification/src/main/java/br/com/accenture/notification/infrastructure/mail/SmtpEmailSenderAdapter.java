package br.com.accenture.notification.infrastructure.mail;

import br.com.accenture.notification.application.port.EmailSender;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
public class SmtpEmailSenderAdapter implements EmailSender {

    private static final String LOGO_RESOURCE = "images/accestore_logo.png";
    private static final String LOGO_CID = "accestoreLogo";
    private static final String WELCOME_SUBJECT = "Bem-vindo ao AcceStore!";
    private static final String WELCOME_HTML = """
            <!DOCTYPE html>
            <html lang="pt-BR">
              <head><meta charset="UTF-8"></head>
              <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                  <div style="text-align:center; margin-bottom:24px;">
                    <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                  </div>
                  <h1 style="color:#1a1a1a; font-size:22px;">Bem-vindo ao AcceStore!</h1>
                  <p style="font-size:15px; line-height:1.5;">
                    Sua conta foi criada com sucesso. Estamos felizes em ter voce conosco.
                  </p>
                  <p style="font-size:15px; line-height:1.5;">
                    Aproveite nossa loja, explore os produtos e bons negocios!
                  </p>
                  <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                  <p style="font-size:12px; color:#888;">
                    Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                  </p>
                </div>
              </body>
            </html>
            """.formatted(LOGO_CID);

    private final JavaMailSender mailSender;
    private final String fromAddress;

    public SmtpEmailSenderAdapter(JavaMailSender mailSender,
                                  @Value("${spring.mail.username}") String fromAddress) {
        this.mailSender = mailSender;
        this.fromAddress = fromAddress;
    }

    @Override
    public void sendWelcomeEmail(String recipient) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(WELCOME_SUBJECT);
            helper.setText(WELCOME_HTML, true);
            helper.addInline(LOGO_CID, new ClassPathResource(LOGO_RESOURCE));
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Failed to send welcome email to " + recipient, e);
        }
    }
}
