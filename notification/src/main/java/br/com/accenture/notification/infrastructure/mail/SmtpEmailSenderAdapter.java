package br.com.accenture.notification.infrastructure.mail;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.stream.Collectors;

@Component
public class SmtpEmailSenderAdapter implements EmailSender {

    private static final String LOGO_RESOURCE = "images/accestore_logo.png";
    private static final String LOGO_CID = "accestoreLogo";

    private static final String WELCOME_SUBJECT = "Bem-vindo ao AcceStore!";
    private static final String ORDER_CREATED_SUBJECT = "Pedido recebido - AcceStore";
    private static final String ORDER_PAID_SUBJECT = "Pagamento confirmado - AcceStore";
    private static final String ORDER_CANCELED_SUBJECT = "Pedido cancelado - AcceStore";

    private static final NumberFormat BRL = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));

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
        sendHtmlEmail(recipient, WELCOME_SUBJECT, WELCOME_HTML);
    }

    @Override
    public void sendOrderCreatedEmail(String recipient, OrderCreatedEmailData data) {
        sendHtmlEmail(recipient, ORDER_CREATED_SUBJECT, buildOrderCreatedHtml(data));
    }

    @Override
    public void sendOrderPaidEmail(String recipient, String orderId) {
        sendHtmlEmail(recipient, ORDER_PAID_SUBJECT, buildOrderPaidHtml(orderId));
    }

    @Override
    public void sendOrderCanceledEmail(String recipient, String orderId, String reason) {
        sendHtmlEmail(recipient, ORDER_CANCELED_SUBJECT, buildOrderCanceledHtml(orderId, reason));
    }

    private void sendHtmlEmail(String recipient, String subject, String html) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress);
            helper.setTo(recipient);
            helper.setSubject(subject);
            helper.setText(html, true);
            helper.addInline(LOGO_CID, new ClassPathResource(LOGO_RESOURCE));
            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new EmailDeliveryException("Failed to send email to " + recipient, e);
        }
    }

    private static String buildOrderCreatedHtml(OrderCreatedEmailData data) {
        String itemsRows = data.items().stream()
                .map(item -> """
                        <tr>
                          <td style="padding:8px; border-bottom:1px solid #eee;">%s</td>
                          <td style="text-align:right; padding:8px; border-bottom:1px solid #eee;">%d</td>
                        </tr>
                        """.formatted(item.sku(), item.quantity()))
                .collect(Collectors.joining());

        String totalFormatted = BRL.format(data.totalAmount());

        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Pedido recebido!</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        Recebemos seu pedido <strong>%s</strong>. Confira abaixo o resumo:
                      </p>
                      <table style="width:100%%; border-collapse:collapse; margin:16px 0;">
                        <thead>
                          <tr style="background:#f0f0f0;">
                            <th style="text-align:left; padding:8px; border-bottom:1px solid #ddd;">SKU</th>
                            <th style="text-align:right; padding:8px; border-bottom:1px solid #ddd;">Quantidade</th>
                          </tr>
                        </thead>
                        <tbody>
                          %s
                        </tbody>
                      </table>
                      <p style="font-size:16px; line-height:1.5;">
                        <strong>Total:</strong> %s
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, data.orderId(), itemsRows, totalFormatted);
    }

    private static String buildOrderPaidHtml(String orderId) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Pagamento confirmado!</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        O pagamento do pedido <strong>%s</strong> foi confirmado com sucesso.
                      </p>
                      <p style="font-size:15px; line-height:1.5;">
                        Em breve enviaremos atualizacoes sobre o envio.
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, orderId);
    }

    private static String buildOrderCanceledHtml(String orderId, String reason) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Pedido cancelado</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        Seu pedido <strong>%s</strong> foi cancelado.
                      </p>
                      <div style="background:#fff4f4; border-left:4px solid #d9534f; padding:12px 16px; margin:16px 0; font-size:14px;">
                        <strong>Motivo:</strong> %s
                      </div>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, orderId, reason);
    }
}
