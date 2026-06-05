package com.mydestiny.config.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.stereotype.Component;

@Component
public class CustomOAuth2AuthorizationRequestResolver implements OAuth2AuthorizationRequestResolver {

    static final String SESSION_ATTR_REDIRECT_URI = "app_redirect_uri";
    private static final String REDIRECT_URI_PARAM = "redirect_uri";

    private final DefaultOAuth2AuthorizationRequestResolver delegate;

    public CustomOAuth2AuthorizationRequestResolver(ClientRegistrationRepository clientRegistrationRepository) {
        this.delegate = new DefaultOAuth2AuthorizationRequestResolver(
                clientRegistrationRepository, "/oauth2/authorization");
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request);
        storeRedirectUri(request, authRequest);
        return authRequest;
    }

    @Override
    public OAuth2AuthorizationRequest resolve(HttpServletRequest request, String clientRegistrationId) {
        OAuth2AuthorizationRequest authRequest = delegate.resolve(request, clientRegistrationId);
        storeRedirectUri(request, authRequest);
        return authRequest;
    }

    private void storeRedirectUri(HttpServletRequest request, OAuth2AuthorizationRequest authRequest) {
        if (authRequest == null) return;
        String redirectUri = request.getParameter(REDIRECT_URI_PARAM);
        if (redirectUri != null && !redirectUri.isBlank()) {
            request.getSession().setAttribute(SESSION_ATTR_REDIRECT_URI, redirectUri);
        }
    }
}
