package com.mydestiny.service.sms;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "log", matchIfMissing = true)
public class LogSmsService implements SmsService {

    @Override
    public void send(String to, String message) {
        log.info("📱 [SMS-DEV] to={} message={}", to, message);
    }
}
