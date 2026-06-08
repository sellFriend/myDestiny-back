package com.mydestiny.config.oauth2;

import com.mydestiny.config.jwt.JwtTokenProvider;
import com.mydestiny.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @Value("${app.oauth2.redirect-uri:http://localhost:3000/oauth2/callback}")
    private String defaultRedirectUri;

    @Value("${app.oauth2.allowed-redirect-uris:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedRedirectUris;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomOAuth2User oAuth2User = (CustomOAuth2User) authentication.getPrincipal();
        String userId = oAuth2User.getUserId();

        String accessToken = jwtTokenProvider.generateAccessToken(userId);
        String refreshToken = jwtTokenProvider.generateRefreshToken(userId);

        LocalDateTime now = LocalDateTime.now();
        userRepository.updateRefreshToken(userId, refreshToken,
                now.plusSeconds(jwtTokenProvider.getRefreshTokenExpirySeconds()), now);
        String profileImageUrl = userRepository.findKakaoProfileImageUrlById(userId).orElse(null);

        String targetRedirectUri = resolveRedirectUri(request);

        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(targetRedirectUri)
                .queryParam("accessToken", accessToken)
                .queryParam("refreshToken", refreshToken)
                .queryParam("userId", userId);
        if (profileImageUrl != null) {
            builder.queryParam("profileImageUrl", profileImageUrl);
        }
        String targetUrl = builder.build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    private String resolveRedirectUri(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session == null) return defaultRedirectUri;

        String uri = (String) session.getAttribute(CustomOAuth2AuthorizationRequestResolver.SESSION_ATTR_REDIRECT_URI);
        session.removeAttribute(CustomOAuth2AuthorizationRequestResolver.SESSION_ATTR_REDIRECT_URI);

        if (uri == null || uri.isBlank()) return defaultRedirectUri;

        boolean allowed = allowedRedirectUris.stream().anyMatch(uri::startsWith);
        return allowed ? uri : defaultRedirectUri;
    }
}
