package br.com.accenture.notification.infrastructure.mail;

import br.com.accenture.notification.application.port.data.OrderCreatedEmailData;
import br.com.accenture.notification.domain.enums.PaymentMethod;
import jakarta.mail.Message;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SmtpEmailSenderAdapterTest {

    private static final String FROM_ADDRESS = "noreply@accestore.com";
    private static final String RECIPIENT = "user@example.com";

    private final JavaMailSender mailSender = mock(JavaMailSender.class);
    private final SmtpEmailSenderAdapter adapter = new SmtpEmailSenderAdapter(mailSender, FROM_ADDRESS);

    @Test
    void sendWelcomeEmailSetsHeadersAndIncludesHtmlAndLogo() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendWelcomeEmail(RECIPIENT);

        MimeMessage sent = captureSent();
        assertThat(sent.getFrom()[0].toString()).isEqualTo(FROM_ADDRESS);
        assertThat(sent.getRecipients(Message.RecipientType.TO)[0].toString()).isEqualTo(RECIPIENT);
        assertThat(sent.getSubject()).isEqualTo("Bem-vindo ao AcceStore!");
        String raw = extractRaw(sent);
        assertThat(raw).contains("Bem-vindo ao AcceStore!");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendOrderCreatedEmailIncludesOrderIdItemsAndFormattedTotal() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());
        OrderCreatedEmailData data = new OrderCreatedEmailData(
                "ORDER-42",
                new BigDecimal("300.00"),
                List.of(
                        new OrderCreatedEmailData.Item("PROD-999", 2),
                        new OrderCreatedEmailData.Item("PROD-888", 5)
                )
        );

        adapter.sendOrderCreatedEmail(RECIPIENT, data);

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Pedido recebido - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-42");
        assertThat(raw).contains("PROD-999").contains("PROD-888");
        assertThat(raw).contains("R$").contains("300,00");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendOrderPaidEmailIncludesOrderId() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendOrderPaidEmail(RECIPIENT, "ORDER-77");

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Pagamento confirmado - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-77");
        assertThat(raw).contains("Pagamento confirmado!");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendOrderCanceledEmailIncludesOrderIdAndReason() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendOrderCanceledEmail(RECIPIENT, "ORDER-99", "Pagamento recusado");

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Pedido cancelado - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-99");
        assertThat(raw).contains("Pagamento recusado");
        assertThat(raw).contains("Pedido cancelado");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendPaymentRefusedEmailIncludesOrderIdAmountMethodAndReason() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendPaymentRefusedEmail(RECIPIENT, "ORDER-101", new BigDecimal("250.00"),
                PaymentMethod.CREDIT_CARD, "Cartao sem limite");

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Pagamento recusado - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-101");
        assertThat(raw).contains("R$").contains("250,00");
        assertThat(raw).contains("Cartao de credito");
        assertThat(raw).contains("Cartao sem limite");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendPaymentCanceledEmailIncludesOrderIdAmountMethodAndReason() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendPaymentCanceledEmail(RECIPIENT, "ORDER-202", new BigDecimal("99.90"),
                PaymentMethod.PIX, "Cancelado pelo cliente");

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Pagamento cancelado - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-202");
        assertThat(raw).contains("R$").contains("99,90");
        assertThat(raw).contains("PIX");
        assertThat(raw).contains("Cancelado pelo cliente");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void sendStockReservationFailedEmailIncludesOrderIdSkuQuantityAndReason() throws Exception {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());

        adapter.sendStockReservationFailedEmail(RECIPIENT, "ORDER-303", "PROD-555", 4, "Estoque insuficiente");

        MimeMessage sent = captureSent();
        assertThat(sent.getSubject()).isEqualTo("Nao foi possivel reservar seu pedido - AcceStore");
        String raw = extractRaw(sent);
        assertThat(raw).contains("ORDER-303");
        assertThat(raw).contains("PROD-555");
        assertThat(raw).contains("Estoque insuficiente");
        assertThat(raw).contains("cid:accestoreLogo");
    }

    @Test
    void wrapsMailExceptionInEmailDeliveryException() {
        when(mailSender.createMimeMessage()).thenReturn(emptyMimeMessage());
        doThrow(new MailSendException("smtp down")).when(mailSender).send(any(MimeMessage.class));

        assertThatExceptionOfType(EmailDeliveryException.class)
                .isThrownBy(() -> adapter.sendWelcomeEmail(RECIPIENT))
                .withMessageContaining(RECIPIENT)
                .withCauseInstanceOf(MailSendException.class);
    }

    private MimeMessage captureSent() {
        ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(captor.capture());
        return captor.getValue();
    }

    private static MimeMessage emptyMimeMessage() {
        return new MimeMessage(Session.getInstance(new Properties()));
    }

    private static String extractRaw(MimeMessage message) throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        message.writeTo(baos);
        String raw = baos.toString(StandardCharsets.UTF_8);
        return raw.replace("=\r\n", "").replace("=\n", "");
    }
}
