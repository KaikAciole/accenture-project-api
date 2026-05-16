package br.com.accenture.notification.infrastructure.mail;

import br.com.accenture.notification.application.port.EmailSender;
import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import br.com.accenture.notification.domain.enums.PaymentMethod;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
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
    private static final String PAYMENT_REFUSED_SUBJECT = "Pagamento recusado - AcceStore";
    private static final String PAYMENT_CANCELED_SUBJECT = "Pagamento cancelado - AcceStore";
    private static final String STOCK_RESERVATION_FAILED_SUBJECT = "Nao foi possivel reservar seu pedido - AcceStore";
    private static final String PASSWORD_RESET_SUBJECT = "Redefinicao de senha - AcceStore";

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

    @Override
    public void sendPaymentRefusedEmail(String recipient, String orderId, BigDecimal amount, PaymentMethod method, String failureReason) {
        sendHtmlEmail(recipient, PAYMENT_REFUSED_SUBJECT, buildPaymentRefusedHtml(orderId, amount, method, failureReason));
    }

    @Override
    public void sendPaymentCanceledEmail(String recipient, String orderId, BigDecimal amount, PaymentMethod method, String cancellationReason) {
        sendHtmlEmail(recipient, PAYMENT_CANCELED_SUBJECT, buildPaymentCanceledHtml(orderId, amount, method, cancellationReason));
    }

    @Override
    public void sendStockReservationFailedEmail(String recipient, String orderId, String sku, int quantity, String reason) {
        sendHtmlEmail(recipient, STOCK_RESERVATION_FAILED_SUBJECT, buildStockReservationFailedHtml(orderId, sku, quantity, reason));
    }

    @Override
    public void sendPasswordResetEmail(String recipient, String resetLink) {
        sendHtmlEmail(recipient, PASSWORD_RESET_SUBJECT, buildPasswordResetHtml(resetLink));
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

    private static String buildPaymentRefusedHtml(String orderId, BigDecimal amount, PaymentMethod method, String failureReason) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Pagamento recusado</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        Nao conseguimos processar o pagamento do pedido <strong>%s</strong>.
                      </p>
                      <p style="font-size:15px; line-height:1.5;">
                        <strong>Valor:</strong> %s<br/>
                        <strong>Forma de pagamento:</strong> %s
                      </p>
                      <div style="background:#fff4f4; border-left:4px solid #d9534f; padding:12px 16px; margin:16px 0; font-size:14px;">
                        <strong>Motivo:</strong> %s
                      </div>
                      <p style="font-size:15px; line-height:1.5;">
                        Voce pode tentar novamente acessando seu pedido em nossa loja.
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, orderId, BRL.format(amount), method.getLabel(), failureReason);
    }

    private static String buildPaymentCanceledHtml(String orderId, BigDecimal amount, PaymentMethod method, String cancellationReason) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Pagamento cancelado</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        O pagamento do pedido <strong>%s</strong> foi cancelado.
                      </p>
                      <p style="font-size:15px; line-height:1.5;">
                        <strong>Valor:</strong> %s<br/>
                        <strong>Forma de pagamento:</strong> %s
                      </p>
                      <div style="background:#fffbe6; border-left:4px solid #f0ad4e; padding:12px 16px; margin:16px 0; font-size:14px;">
                        <strong>Motivo:</strong> %s
                      </div>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, orderId, BRL.format(amount), method.getLabel(), cancellationReason);
    }

    private static String buildPasswordResetHtml(String resetLink) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Redefinicao de senha</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        Recebemos uma solicitacao para redefinir a senha da sua conta no AcceStore.
                      </p>
                      <p style="font-size:15px; line-height:1.5;">
                        Clique no botao abaixo para escolher uma nova senha. O link e valido por tempo limitado.
                      </p>
                      <div style="text-align:center; margin:32px 0;">
                        <a href="%s" style="background:#1a73e8; color:#ffffff; padding:12px 24px; text-decoration:none; border-radius:4px; display:inline-block; font-weight:bold;">
                          Redefinir minha senha
                        </a>
                      </div>
                      <p style="font-size:13px; line-height:1.5; color:#555;">
                        Se voce nao solicitou esta mudanca, basta ignorar este e-mail. Sua senha permanecera a mesma.
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, resetLink);
    }

    private static String buildStockReservationFailedHtml(String orderId, String sku, int quantity, String reason) {
        return """
                <!DOCTYPE html>
                <html lang="pt-BR">
                  <head><meta charset="UTF-8"></head>
                  <body style="font-family: Arial, sans-serif; color: #222; background:#f7f7f7; padding:24px;">
                    <div style="max-width:560px; margin:0 auto; background:#ffffff; padding:32px; border-radius:8px;">
                      <div style="text-align:center; margin-bottom:24px;">
                        <img src="cid:%s" alt="AcceStore" style="max-width:220px; height:auto;"/>
                      </div>
                      <h1 style="color:#1a1a1a; font-size:22px;">Nao foi possivel reservar seu pedido</h1>
                      <p style="font-size:15px; line-height:1.5;">
                        Tivemos um problema ao reservar os itens do pedido <strong>%s</strong>.
                      </p>
                      <p style="font-size:15px; line-height:1.5;">
                        <strong>SKU:</strong> %s<br/>
                        <strong>Quantidade:</strong> %d
                      </p>
                      <div style="background:#fff4f4; border-left:4px solid #d9534f; padding:12px 16px; margin:16px 0; font-size:14px;">
                        <strong>Motivo:</strong> %s
                      </div>
                      <p style="font-size:15px; line-height:1.5;">
                        Pedimos desculpas pelo inconveniente. Voce pode tentar novamente acessando seu pedido.
                      </p>
                      <hr style="border:none; border-top:1px solid #eee; margin:24px 0;"/>
                      <p style="font-size:12px; color:#888;">
                        Esta e uma mensagem automatica do sistema AcceStore. Por favor, nao responda.
                      </p>
                    </div>
                  </body>
                </html>
                """.formatted(LOGO_CID, orderId, sku, quantity, reason);
    }
}
