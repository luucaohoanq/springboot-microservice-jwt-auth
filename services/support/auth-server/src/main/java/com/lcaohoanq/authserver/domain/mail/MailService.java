package com.lcaohoanq.authserver.domain.mail;

import com.lcaohoanq.commonlibrary.dto.UserResponse;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.MessageSource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import reactor.core.publisher.Mono;

/** Service for sending emails asynchronously. */
@Service
@RequiredArgsConstructor
public class MailService {

  private static final Logger LOG = LoggerFactory.getLogger(MailService.class);
  private static final String USER = "user";
  private static final String BASE_URL = "baseUrl";

  @Value("${spring.mail.username}")
  private String sendFrom;

  @Value("${spring.mail.base-url}")
  private String baseUrl;

  private final JavaMailSender javaMailSender;
  private final MessageSource messageSource;
  private final SpringTemplateEngine templateEngine;

  public void sendEmail(
      String to, String subject, String content, boolean isMultipart, boolean isHtml) {
    Mono.defer(
            () -> {
              sendEmailSync(to, subject, content, isMultipart, isHtml);
              return Mono.empty();
            })
        .subscribe();
  }

  private void sendEmailSync(
      String to, String subject, String content, boolean isMultipart, boolean isHtml) {
    LOG.debug(
        "Send email[multipart '{}' and html '{}'] to '{}' with subject '{}' and content={}",
        isMultipart,
        isHtml,
        to,
        subject,
        content);

    // Prepare message using a Spring helper
    MimeMessage mimeMessage = javaMailSender.createMimeMessage();
    try {
      MimeMessageHelper message =
          new MimeMessageHelper(mimeMessage, isMultipart, StandardCharsets.UTF_8.name());
      message.setTo(to);
      message.setFrom(sendFrom);
      message.setSubject(subject);
      message.setText(content, isHtml);
      javaMailSender.send(mimeMessage);
      LOG.debug("Sent email to User '{}'", to);
    } catch (MailException | MessagingException e) {
      LOG.warn("Email could not be sent to user '{}'", to, e);
    }
  }

  public void sendEmailFromTemplate(UserResponse user, String templateName, String titleKey) {
    Mono.defer(
            () -> {
              sendEmailFromTemplateSync(user, templateName, titleKey);
              return Mono.empty();
            })
        .subscribe();
  }

  private void sendEmailFromTemplateSync(UserResponse user, String templateName, String titleKey) {
    if (user.email() == null) {
      LOG.debug("Email doesn't exist for user '{}'", (Object) null);
      return;
    }
    Locale locale = Locale.forLanguageTag(user.langKey() != null ? user.langKey() : "en");
    Context context = new Context(locale);
    context.setVariable(USER, user);
    context.setVariable(BASE_URL, baseUrl);
    String content = templateEngine.process(templateName, context);
    String subject = messageSource.getMessage(titleKey, null, locale);
    sendEmailSync(user.email(), subject, content, false, true);
  }

  public void sendActivationEmail(UserResponse user) {
    LOG.debug("Sending activation email to '{}'", user.email());
    sendEmailFromTemplate(user, "mail/activationEmail", "email.activation.title");
  }

  public void sendCreationEmail(UserResponse user) {
    LOG.debug("Sending creation email to '{}'", user.email());
    sendEmailFromTemplate(user, "mail/creationEmail", "email.activation.title");
  }

  public void sendPasswordResetMail(UserResponse user) {
    LOG.debug("Sending password reset email to '{}'", user.email());
    sendEmailFromTemplate(user, "mail/passwordResetEmail", "email.reset.title");
  }
}
