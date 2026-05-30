package com.mydestiny.controller;

import com.mydestiny.config.jwt.JwtTokenProvider;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.Role;
import com.mydestiny.repository.UserRepository;
import com.mydestiny.util.PhoneEncryptionUtil;
import com.mydestiny.util.PhoneHashUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@Transactional
class UserControllerTest {

    @Autowired WebApplicationContext context;
    @Autowired UserRepository userRepository;
    @Autowired JwtTokenProvider jwtTokenProvider;
    @Autowired PhoneHashUtil phoneHashUtil;
    @Autowired PhoneEncryptionUtil phoneEncryptionUtil;

    private MockMvc mockMvc;
    private String accessToken;
    private User testUser;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        testUser = userRepository.save(User.builder()
                .kakaoId("kakao-test-001")
                .email("test@mydestiny.com")
                .nickname("테스트유저")
                .build());
        accessToken = jwtTokenProvider.generateAccessToken(testUser.getId());
    }

    @Test
    void getMe_정상_응답() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.nickname").value("테스트유저"))
                .andExpect(jsonPath("$.data.role").value("USER"))
                .andExpect(jsonPath("$.data.phoneVerified").value(false))
                .andExpect(jsonPath("$.data.maskedPhone").isEmpty());
    }

    @Test
    void getMe_전화번호_인증_후_마스킹_표시() throws Exception {
        testUser.updatePhoneNumber(
                phoneHashUtil.hash("01012345678"),
                phoneEncryptionUtil.encrypt("01012345678")
        );
        userRepository.save(testUser);

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.phoneVerified").value(true))
                .andExpect(jsonPath("$.data.maskedPhone").value("010-****-5678"));
    }

    @Test
    void getMe_토큰_없으면_401() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getMe_어드민_역할_확인() throws Exception {
        User admin = userRepository.save(User.builder()
                .kakaoId("kakao-admin-001")
                .email("admin@mydestiny.com")
                .nickname("관리자")
                .role(Role.ADMIN)
                .build());
        String adminToken = jwtTokenProvider.generateAccessToken(admin.getId());

        mockMvc.perform(get("/api/users/me")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.role").value("ADMIN"));
    }
}
