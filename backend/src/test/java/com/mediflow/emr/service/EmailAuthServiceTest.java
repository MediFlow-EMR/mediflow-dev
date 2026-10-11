package com.mediflow.emr.service;

import com.mediflow.emr.dto.EmailSignupRequest;
import com.mediflow.emr.entity.*;
import com.mediflow.emr.entity.enums.Provider;
import com.mediflow.emr.exception.*;
import com.mediflow.emr.repository.*;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailAuthenticationException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Properties;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailAuthServiceTest {
    private static final String EMAIL = "signup@example.invalid";
    private final UserRepository users = mock(UserRepository.class);
    private final EmailVerificationRepository verifications = mock(EmailVerificationRepository.class);
    private final JavaMailSender mail = mock(JavaMailSender.class);
    private final PasswordEncoder passwords = mock(PasswordEncoder.class);
    private final DummyDataService demo = mock(DummyDataService.class);
    private final EmailAuthService service = new EmailAuthService(users, verifications, mail, passwords, demo);

    private User user(Provider provider, String password) {
        return User.builder().id(1L).email(EMAIL).provider(provider).providerId(EMAIL).password(password).build();
    }
    private EmailVerification verification(LocalDateTime expiry) {
        var verification = EmailVerification.builder().email(EMAIL).verificationCode("1234")
                .expiryDate(expiry).user(user(Provider.LOCAL, null)).build();
        when(verifications.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(verification));
        when(verifications.findByEmail(EMAIL)).thenReturn(Optional.of(verification));
        return verification;
    }
    @Test void checkingAvailableEmailDoesNotSendOrCreateAnything() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.empty());
        assertThat(service.checkEmailDuplicate(EMAIL)).isFalse();
        verifyNoInteractions(mail, verifications);
        verify(users, never()).save(any());
    }
    @Test void incompleteLocalAccountIsAvailableWithoutResending() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(Provider.LOCAL, null)));
        assertThat(service.checkEmailDuplicate(EMAIL)).isFalse();
        verifyNoInteractions(mail, verifications);
    }
    @Test void socialAccountWithoutPasswordIsStillDuplicate() {
        when(users.findByEmail(EMAIL)).thenReturn(Optional.of(user(Provider.GOOGLE, null)));
        assertThat(service.checkEmailDuplicate(EMAIL)).isTrue();
    }
    @Test void completedAccountCannotRequestSignupCode() {
        when(users.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user(Provider.LOCAL, "encoded")));
        assertThatThrownBy(() -> service.sendCode(EMAIL)).isInstanceOf(BusinessException.class);
        verifyNoInteractions(mail, verifications);
    }
    @Test void resendIsLimitedToOncePerMinute() {
        when(users.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user(Provider.LOCAL, null)));
        verification(LocalDateTime.now().plusMinutes(5));
        assertThatThrownBy(() -> service.sendCode(EMAIL)).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.TOO_MANY_REQUESTS);
        verifyNoInteractions(mail);
    }
    @Test void smtpFailureReturnsRecoverableServiceUnavailable() {
        when(users.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user(Provider.LOCAL, null)));
        when(verifications.findByEmailForUpdate(EMAIL)).thenReturn(Optional.empty());
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        ReflectionTestUtils.setField(service, "mailHost", "sender@example.invalid");
        doThrow(new MailAuthenticationException("test only")).when(mail).send(any(MimeMessage.class));
        assertThatThrownBy(() -> service.sendCode(EMAIL)).isInstanceOf(BusinessException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.MAIL_UNAVAILABLE);
        verify(verifications, never()).save(any());
    }
    @Test void wrongCodeDoesNotResendAndFiveFailuresLockVerification() {
        var verification = verification(LocalDateTime.now().plusMinutes(5));
        for (int i=0; i<5; i++) assertThat(service.isMatchCode(EMAIL, "9999")).isFalse();
        assertThat(verification.getFailedAttempts()).isEqualTo(5);
        assertThat(service.isMatchCode(EMAIL, "1234")).isFalse();
        assertThat(service.getRemainingTime(EMAIL)).isZero();
        verifyNoInteractions(mail);
    }
    @Test void validCodeVerifiesChallengeAndCannotBeReplayed() {
        var verification = verification(LocalDateTime.now().plusMinutes(5));
        assertThat(service.isMatchCode(EMAIL, "1234")).isTrue();
        assertThat(service.isMatchCode(EMAIL, "1234")).isFalse();
        assertThat(verification.getUser().isEmailVerified()).isFalse();
        verifyNoInteractions(mail);
    }
    @Test void expiredVerifiedChallengeCannotFinishSignup() {
        var verification = verification(LocalDateTime.now().minusSeconds(1));
        verification.verify();
        when(users.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(verification.getUser()));
        assertThatThrownBy(() -> service.completeSignup(new EmailSignupRequest(EMAIL, "test")))
                .isInstanceOf(BusinessException.class);
        verifyNoInteractions(passwords, demo);
    }
    @Test void successfulExplicitResendResetsFailedAttempts() {
        when(users.findByEmailForUpdate(EMAIL)).thenReturn(Optional.of(user(Provider.LOCAL, null)));
        var verification = verification(LocalDateTime.now().plusMinutes(3));
        verification.recordFailedAttempt();
        when(mail.createMimeMessage()).thenReturn(new MimeMessage(Session.getInstance(new Properties())));
        ReflectionTestUtils.setField(service, "mailHost", "sender@example.invalid");
        service.sendCode(EMAIL);
        assertThat(verification.getFailedAttempts()).isZero();
        assertThat(verification.getExpiryDate()).isAfter(LocalDateTime.now().plusMinutes(4));
        verify(mail).send(any(MimeMessage.class));
    }
}
