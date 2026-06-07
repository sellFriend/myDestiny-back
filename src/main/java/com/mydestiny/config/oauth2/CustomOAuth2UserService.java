package com.mydestiny.config.oauth2;

import com.mydestiny.domain.User;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oAuth2User.getAttributes();

        // 카카오 응답 구조: { id: Long, kakao_account: { email, profile: { nickname } } }
        String kakaoId = String.valueOf(attributes.get("id"));

        @SuppressWarnings("unchecked")
        Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
        String email = kakaoAccount != null ? (String) kakaoAccount.get("email") : null;

        @SuppressWarnings("unchecked")
        Map<String, Object> profile = kakaoAccount != null
                ? (Map<String, Object>) kakaoAccount.get("profile") : null;
        String nickname = (profile != null && profile.get("nickname") != null)
                ? (String) profile.get("nickname")
                : (email != null ? email.split("@")[0] : kakaoId);
        String profileImageUrl = (profile != null) ? (String) profile.get("profile_image_url") : null;

        String userId = userRepository.findByKakaoId(kakaoId)
                .map(existing -> {
                    userRepository.touchLogin(existing.getId(), profileImageUrl, LocalDateTime.now());
                    return existing.getId();
                })
                .orElseGet(() -> userRepository.save(
                        User.builder()
                                .kakaoId(kakaoId)
                                .email(email)
                                .nickname(nickname)
                                .kakaoProfileImageUrl(profileImageUrl)
                                .build()
                ).getId());

        return new CustomOAuth2User(userId, attributes);
    }
}
