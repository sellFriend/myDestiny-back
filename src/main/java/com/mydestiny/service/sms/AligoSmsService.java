package com.mydestiny.service.sms;

import com.mydestiny.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "aligo")
public class AligoSmsService implements SmsService {

    private static final String ALIGO_URL = "https://apis.aligo.in/send/";

    private final String apiKey;
    private final String userId;
    private final String senderNo;
    private final RestClient restClient;

    public AligoSmsService(
            @Value("${app.sms.aligo.key}") String apiKey,
            @Value("${app.sms.aligo.user-id}") String userId,
            @Value("${app.sms.aligo.sender-no}") String senderNo) {
        this.apiKey = apiKey;
        this.userId = userId;
        this.senderNo = senderNo;
        this.restClient = RestClient.create();
    }

    @Override
    public void send(String to, String message) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("key", apiKey);
        form.add("user_id", userId);
        form.add("sender", senderNo);
        form.add("receiver", to);
        form.add("msg", message);

        try {
            Map<?, ?> response = restClient.post()
                    .uri(ALIGO_URL)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(Map.class);

            String resultCode = response != null ? String.valueOf(response.get("result_code")) : "-1";
            if (Integer.parseInt(resultCode) < 1) {
                String message2 = response != null ? String.valueOf(response.get("message")) : "unknown";
                log.error("알리고 SMS 발송 실패: code={} message={}", resultCode, message2);
                throw new BusinessException("SMS 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            log.info("알리고 SMS 발송 성공: to={}", to);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("알리고 SMS 연동 오류: {}", e.getMessage());
            throw new BusinessException("SMS 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
