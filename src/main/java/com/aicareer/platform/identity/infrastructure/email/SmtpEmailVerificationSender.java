package com.aicareer.platform.identity.infrastructure.email;

import java.time.Duration;
import java.util.Properties;

import com.aicareer.platform.identity.application.EmailVerificationException;
import com.aicareer.platform.identity.application.EmailVerificationMessageSender;
import com.aicareer.platform.identity.application.EmailVerificationProperties;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(
        prefix = "app.identity.email-verification",
        name = "provider",
        havingValue = "smtp",
        matchIfMissing = true
)
public class SmtpEmailVerificationSender implements EmailVerificationMessageSender {

    private final JavaMailSenderImpl mailSender;
    private final EmailVerificationProperties properties;

    public SmtpEmailVerificationSender(EmailVerificationProperties properties) {
        this.properties = properties;
        this.mailSender = createMailSender(properties);
    }

    @Override
    public void sendVerificationCode(String recipient, String code, Duration validFor) {
        if (!properties.enabled()) {
            throw new IllegalStateException("Email delivery is disabled");
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, false, "UTF-8");
            helper.setFrom(properties.fromAddress(), properties.fromName());
            helper.setTo(recipient);
            helper.setSubject("Career Note 이메일 인증 코드");
            long validMinutes = Math.max(1, validFor.toMinutes());
            helper.setText("""
                    <div style="font-family:Arial,sans-serif;max-width:520px;margin:0 auto;padding:32px;color:#27342a">
                      <p style="font-size:12px;color:#66805f">CAREER NOTE</p>
                      <h1 style="font-size:24px">이메일을 확인해주세요</h1>
                      <p>회원가입을 계속하려면 아래 인증코드를 입력해주세요.</p>
                      <p style="margin:28px 0;font-size:34px;font-weight:700;letter-spacing:8px;color:#496b3e">%s</p>
                      <p style="font-size:13px;color:#6f7b71">이 코드는 %d분 동안 유효합니다. 직접 요청하지 않았다면 이 메일을 무시해주세요.</p>
                    </div>
                    """.formatted(code, validMinutes), true);
            mailSender.send(message);
        } catch (MessagingException | MailException exception) {
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.DELIVERY_UNAVAILABLE,
                    "The verification email could not be delivered",
                    exception
            );
        } catch (java.io.UnsupportedEncodingException exception) {
            throw new EmailVerificationException(
                    EmailVerificationException.Kind.DELIVERY_UNAVAILABLE,
                    "The verification email could not be prepared",
                    exception
            );
        }
    }

    static JavaMailSenderImpl createMailSender(EmailVerificationProperties properties) {
        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(properties.smtpHost());
        sender.setPort(properties.smtpPort());
        sender.setUsername(properties.smtpUsername());
        sender.setPassword(properties.smtpPassword());
        sender.setDefaultEncoding("UTF-8");
        Properties javaMailProperties = sender.getJavaMailProperties();
        javaMailProperties.put("mail.smtp.auth", "true");
        javaMailProperties.put("mail.smtp.starttls.enable", Boolean.toString(properties.smtpStartTlsEnabled()));
        javaMailProperties.put("mail.smtp.starttls.required", Boolean.toString(properties.smtpStartTlsEnabled()));
        javaMailProperties.put("mail.smtp.ssl.enable", Boolean.toString(properties.smtpSslEnabled()));
        javaMailProperties.put("mail.smtp.connectiontimeout", "5000");
        javaMailProperties.put("mail.smtp.timeout", "5000");
        javaMailProperties.put("mail.smtp.writetimeout", "5000");
        return sender;
    }
}
