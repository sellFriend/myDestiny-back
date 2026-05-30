package com.mydestiny.service.sms;

import com.mydestiny.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Primary
@ConditionalOnProperty(name = "app.sms.provider", havingValue = "nhn")
public class NhnCloudSmsService implements SmsService {

    private final RestClient restClient;
    private final String appKey;
    private final String senderNo;

    public NhnCloudSmsService(
            @Value("${app.sms.nhn.app-key}") String appKey,
            @Value("${app.sms.nhn.secret-key}") String secretKey,
            @Value("${app.sms.nhn.sender-no}") String senderNo) {
        this.appKey = appKey;
        this.senderNo = senderNo;
        this.restClient = RestClient.builder()
                .baseUrl("https://api-sms.cloud.toast.com")
                .defaultHeader("X-Secret-Key", secretKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    @Override
    public void send(String to, String message) {
        Map<String, Object> body = Map.of(
                "body", message,
                "sendNo", senderNo,
                "recipientList", List.of(Map.of("recipientNo", to))
        );

        try {
            Map<?, ?> response = restClient.post()
                    .uri("/sms/v3.0/appKeys/{appKey}/sender/sms", appKey)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Map<?, ?> header = response != null ? (Map<?, ?>) response.get("header") : null;
            boolean success = header != null && Boolean.TRUE.equals(header.get("isSuccessful"));

            if (!success) {
                String resultMessage = header != null ? String.valueOf(header.get("resultMessage")) : "unknown";
                log.error("NHN Cloud SMS 발송 실패: {}", resultMessage);
                throw new BusinessException("SMS 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
            }

            log.info("SMS 발송 성공: to={}", to);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("NHN Cloud SMS 연동 오류: {}", e.getMessage());
            throw new BusinessException("SMS 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.", HttpStatus.SERVICE_UNAVAILABLE);
        }
    }
}
