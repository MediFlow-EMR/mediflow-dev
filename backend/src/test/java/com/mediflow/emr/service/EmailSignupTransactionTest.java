package com.mediflow.emr.service;

import com.mediflow.emr.exception.BusinessException;
import com.mediflow.emr.repository.UserRepository;
import com.mediflow.emr.repository.EmailVerificationRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
class EmailSignupTransactionTest {
    @Autowired EmailAuthService service;
    @Autowired UserRepository users;
    @Autowired EmailVerificationRepository verifications;
    @MockitoBean JavaMailSender mail;

    @Test void smtpFailureRollsBackTemporaryUserAndChallenge() {
        String email = "rollback@example.invalid";
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        doThrow(new MailAuthenticationException("test only")).when(mail).send(any(MimeMessage.class));
        assertThatThrownBy(() -> service.sendCode(email)).isInstanceOf(BusinessException.class);
        assertThat(users.existsByEmail(email)).isFalse();
        assertThat(verifications.existsByEmail(email)).isFalse();
    }
}
