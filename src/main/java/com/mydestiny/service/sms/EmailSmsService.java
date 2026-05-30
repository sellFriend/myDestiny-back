package com.mydestiny.service.sms;

import com.mydestiny.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@Primary
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "email")
public class EmailSmsService implements SmsService {

    private final JavaMailSender mailSender;

    @Value("${app.sms.email.from}")
    private String fromEmail;

    @Override
    public void send(String to, String message) {
        try {
            SimpleMailMessage mail = new SimpleMailMessage();
            mail.setFrom(fromEmail);
            mail.setTo(to);   // B가 입력한 이메일 주소로 발송
            mail.setSubject("[My Destiny] OTP 인증번호");
            mail.setText(message);
            mailSender.send(mail);
            log.info("이메일 OTP 발송: to={}", to);
        } catch (Exception e) {
            log.error("이메일 발송 실패: {}", e.getMessage());
            throw new BusinessException("인증번호 발송에 실패했습니다.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
