package com.mydestiny.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Slf4j
@Component
public class OAuth2FailureHandler extends SimpleUrlAuthenticationFailureHandler {

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String redirectUri;

    @Override
    public void onAuthenticationFailure(HttpServletRequest request, HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        // 실제 실패 원인을 로그로 남긴다 (기본 핸들러는 조용히 /login?error 로 넘겨버려 원인이 사라짐)
        if (exception instanceof OAuth2AuthenticationException oauthEx) {
            OAuth2Error error = oauthEx.getError();
            log.error("OAuth2 로그인 실패 - code={}, description={}, uri={}",
                    error.getErrorCode(), error.getDescription(), error.getUri(), exception);
        } else {
            log.error("OAuth2 로그인 실패", exception);
        }

        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("error", "login_failed")
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
