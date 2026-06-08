package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.Matching;
import com.mydestiny.domain.MatchLog;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ConsentStatus;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.Role;
import com.mydestiny.dto.matching.MatchingRequest;
import com.mydestiny.dto.matching.MatchingResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.MatchCandidateConsentRepository;
import com.mydestiny.repository.MatchLogRepository;
import com.mydestiny.repository.MatchingRepository;
import com.mydestiny.util.PhoneEncryptionUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MatchingServiceTest {

    @Mock MatchingRepository matchingRepository;
    @Mock MatchCandidateConsentRepository consentRepository;
    @Mock MatchLogRepository matchLogRepository;
    @Mock DatingProfileRepository profileRepository;
    @Mock NotificationService notificationService;
    @Mock PhoneEncryptionUtil phoneEncryptionUtil;

    @InjectMocks MatchingService matchingService;

    private User requester;   // A
    private User receiver;    // C
    private User candidateB;  // B (requesterProfile.subject)
    private User candidateD;  // D (targetProfile.subject)
    private DatingProfile requesterProfile;  // B의 프로필
    private DatingProfile targetProfile;     // D의 프로필

    @BeforeEach
    void setUp() {
        requester = User.builder()
                .id("user-a").kakaoId("kakao-a").email("a@test.com")
                .nickname("A").role(Role.USER).build();
        receiver = User.builder()
                .id("user-c").kakaoId("kakao-c").email("c@test.com")
                .nickname("C").role(Role.USER).build();
        candidateB = User.builder()
                .id("user-b").kakaoId("kakao-b").email("b@test.com")
                .nickname("B").role(Role.USER).build();
        candidateD = User.builder()
                .id("user-d").kakaoId("kakao-d").email("d@test.com")
                .nickname("D").role(Role.USER).build();

        requesterProfile = DatingProfile.builder()
                .id("profile-b").registrant(requester).subject(candidateB)
                .status(ProfileStatus.PUBLISHED).name("B").subjectPhoneHash("hash-b").build();
        targetProfile = DatingProfile.builder()
                .id("profile-d").registrant(receiver).subject(candidateD)
                .status(ProfileStatus.PUBLISHED).name("D").subjectPhoneHash("hash-d").build();
    }

    // ────── createMatching ──────

    @Nested
    @DisplayName("createMatching")
    class CreateMatching {

        private MatchingRequest validRequest() {
            return new MatchingRequest("profile-b", "profile-d", "안녕하세요");
        }

        private void stubHappyPath() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            given(matchingRepository.isProfileInActiveMatch(anyString(), anyList())).willReturn(false);
            given(matchingRepository.existsRecentRejection(anyString(), anyString(), anyList(), any())).willReturn(false);
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("정상 요청 시 PENDING 상태의 매칭이 생성된다")
        void success() {
            stubHappyPath();

            MatchingResponse result = matchingService.createMatching("user-a", validRequest());

            assertThat(result.status()).isEqualTo(MatchingStatus.PENDING.name());
            verify(matchingRepository).save(any(Matching.class));
            verify(notificationService).create(eq("user-c"), any(), any());
        }

        @Test
        @DisplayName("V3: 내 프로필이 아니면 403 예외")
        void failV3_notMyProfile() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));

            assertThatThrownBy(() -> matchingService.createMatching("user-x", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("V2: 내 프로필이 PUBLISHED가 아니면 400 예외")
        void failV2_notPublished() {
            DatingProfile draftProfile = DatingProfile.builder()
                    .id("profile-b").registrant(requester).status(ProfileStatus.DRAFT)
                    .name("B").subjectPhoneHash("hash").build();
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(draftProfile));

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("V4: 자기 자신 프로필에 요청하면 400 예외")
        void failV4_selfRequest() {
            // A가 자신이 등록자인 D 프로필에도 요청 → receiver == requester
            DatingProfile ownTarget = DatingProfile.builder()
                    .id("profile-d").registrant(requester) // 등록자가 requester(A)
                    .subject(candidateD).status(ProfileStatus.PUBLISHED)
                    .name("D").subjectPhoneHash("hash-d").build();

            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(ownTarget));

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.BAD_REQUEST);
        }

        @Test
        @DisplayName("V5: 동일 프로필 조합에 활성 매칭이 있으면 409 예외")
        void failV5_duplicateActivePair() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(true);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("프로필 당 1개 활성 매칭 제한: B가 다른 매칭 중이면 409 예외")
        void failProfileActiveMatchLimit() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            given(matchingRepository.isProfileInActiveMatch(eq("profile-b"), anyList())).willReturn(true);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("매칭 성사된 프로필로는 다른 사람에게 요청을 보낼 수 없다 (점유 검사에 MATCHED 포함)")
        void failProfileAlreadyMatched() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            ArgumentCaptor<List<MatchingStatus>> statusCaptor = ArgumentCaptor.forClass(List.class);
            // 이미 매칭 성사된 내 친구 프로필
            given(matchingRepository.isProfileInActiveMatch(eq("profile-b"), anyList())).willReturn(true);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);

            verify(matchingRepository).isProfileInActiveMatch(eq("profile-b"), statusCaptor.capture());
            assertThat(statusCaptor.getValue()).contains(MatchingStatus.MATCHED);
        }

        @Test
        @DisplayName("보낸 요청 1건 제한: 내 친구 프로필이 이미 요청을 보낸 상태면 409 예외 (A→B, A→C 차단)")
        void failOutgoingRequestLimit() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            given(matchingRepository.isProfileInActiveMatch(anyString(), anyList())).willReturn(false);
            // 내 친구 프로필(B)이 이미 다른 PENDING 요청을 보낸 상태
            given(matchingRepository.existsByRequesterProfileIdAndStatus("profile-b", MatchingStatus.PENDING))
                    .willReturn(true);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("V6: 30일 쿨다운 기간이면 409 예외")
        void failV6_cooldown() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            given(matchingRepository.isProfileInActiveMatch(anyString(), anyList())).willReturn(false);
            given(matchingRepository.existsRecentRejection(anyString(), anyString(), anyList(), any())).willReturn(true);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @Disabled("MatchingService 일일 한도 검사 주석 처리 중 — TODO 해제 후 활성화")
        @DisplayName("V12: 일일 한도 초과 시 429 예외")
        void failV12_dailyLimit() {
            given(profileRepository.findById("profile-b")).willReturn(Optional.of(requesterProfile));
            given(profileRepository.findById("profile-d")).willReturn(Optional.of(targetProfile));
            given(matchingRepository.existsActiveMatchBetween(anyString(), anyString(), anyList())).willReturn(false);
            given(matchingRepository.isProfileInActiveMatch(anyString(), anyList())).willReturn(false);
            given(matchingRepository.existsRecentRejection(anyString(), anyString(), anyList(), any())).willReturn(false);
            given(matchingRepository.countByRequesterIdAndCreatedAtBetween(anyString(), any(), any())).willReturn(5);

            assertThatThrownBy(() -> matchingService.createMatching("user-a", validRequest()))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        }
    }

    // ────── acceptMatching ──────

    @Nested
    @DisplayName("acceptMatching")
    class AcceptMatching {

        private Matching pendingMatching() {
            return Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
        }

        @Test
        @DisplayName("수신자 수락 시 MATCHED 상태로 전이되고 요청자·수신자에게 알림 발송")
        void success() {
            Matching matching = pendingMatching();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            MatchingResponse result = matchingService.acceptMatching("match-1", "user-c");

            assertThat(result.status()).isEqualTo(MatchingStatus.MATCHED.name());
            verify(notificationService).create(eq("user-a"), any(), any());
            verify(notificationService).create(eq("user-c"), any(), any());
        }

        @Test
        @DisplayName("성사 시 두 프로필에 엮인 다른 활성 요청들이 자동 취소되고 양측에 알림 발송")
        void cancelsEntangledMatchesOnAccept() {
            Matching matching = pendingMatching();
            // B(profile-b)에게 다른 사용자가 보낸 또 다른 PENDING 요청
            Matching entangled = Matching.builder()
                    .id("match-2").requester(candidateB).receiver(requester)
                    .requesterProfile(targetProfile).targetProfile(requesterProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();

            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchingRepository.findActiveByProfiles(eq("match-1"), anyList(), anyList()))
                    .willReturn(List.of(entangled));

            matchingService.acceptMatching("match-1", "user-c");

            assertThat(entangled.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
            verify(notificationService).create(eq("user-b"), eq(NotificationType.MATCH_CANCELLED), eq("match-2"));
            verify(notificationService).create(eq("user-a"), eq(NotificationType.MATCH_CANCELLED), eq("match-2"));
        }

        @Test
        @DisplayName("수신자가 아닌 사용자가 수락 시 403 예외")
        void failNotReceiver() {
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(pendingMatching()));

            assertThatThrownBy(() -> matchingService.acceptMatching("match-1", "user-x"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("PENDING이 아닌 상태에서 수락 시 409 예외")
        void failAlreadyProcessed() {
            Matching alreadyAccepted = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.ACCEPTED_BY_RECEIVER)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(alreadyAccepted));

            assertThatThrownBy(() -> matchingService.acceptMatching("match-1", "user-c"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }

        @Test
        @DisplayName("수신자 응답 기한 초과 시 410 예외")
        void failExpired() {
            Matching expired = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().minusHours(1)) // 이미 만료
                    .build();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(expired));

            assertThatThrownBy(() -> matchingService.acceptMatching("match-1", "user-c"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.GONE);
        }
    }

    // ────── rejectMatching ──────

    @Nested
    @DisplayName("rejectMatching")
    class RejectMatching {

        private Matching pendingMatching() {
            return Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
        }

        @Test
        @DisplayName("수신자 거절 시 REJECTED_BY_RECEIVER 상태로 전이")
        void success() {
            Matching matching = pendingMatching();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            MatchingResponse result = matchingService.rejectMatching("match-1", "user-c", "거절합니다");

            assertThat(result.status()).isEqualTo(MatchingStatus.REJECTED_BY_RECEIVER.name());
            verify(notificationService).create(eq("user-a"), any(), anyString());
        }

        @Test
        @DisplayName("수신자가 아닌 사용자가 거절 시 403 예외")
        void failNotReceiver() {
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(pendingMatching()));

            assertThatThrownBy(() -> matchingService.rejectMatching("match-1", "user-x", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ────── cancelMatching ──────

    @Nested
    @DisplayName("cancelMatching")
    class CancelMatching {

        @Test
        @DisplayName("요청자가 PENDING 상태에서 취소 시 CANCELLED 상태로 전이")
        void success() {
            Matching matching = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            matchingService.cancelMatching("match-1", "user-a");

            assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CANCELLED);
        }

        @Test
        @DisplayName("요청자가 아닌 사용자가 취소 시 403 예외")
        void failNotRequester() {
            Matching matching = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));

            assertThatThrownBy(() -> matchingService.cancelMatching("match-1", "user-x"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }
    }

    // ────── cancelMatchedMatching ──────

    @Nested
    @DisplayName("cancelMatchedMatching")
    class CancelMatchedMatching {

        private Matching matchedMatching() {
            return Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.MATCHED)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
        }

        @Test
        @DisplayName("요청자가 성사된 매칭 취소 시 CANCELLED_AFTER_MATCH로 전이되고 수신자에게 알림 발송")
        void successByRequester() {
            Matching matching = matchedMatching();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            MatchingResponse result = matchingService.cancelMatchedMatching("match-1", "user-a", "사정이 생겼어요");

            assertThat(result.status()).isEqualTo(MatchingStatus.CANCELLED_AFTER_MATCH.name());
            assertThat(matching.getCancelledBy().getId()).isEqualTo("user-a");
            verify(notificationService).create(eq("user-c"), eq(NotificationType.MATCH_RELEASED), eq("match-1"));
            verify(notificationService, never()).create(eq("user-a"), any(), any());
        }

        @Test
        @DisplayName("수신자가 성사된 매칭 취소 시 요청자에게 알림 발송")
        void successByReceiver() {
            Matching matching = matchedMatching();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matching));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            matchingService.cancelMatchedMatching("match-1", "user-c", null);

            assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CANCELLED_AFTER_MATCH);
            assertThat(matching.getCancelledBy().getId()).isEqualTo("user-c");
            verify(notificationService).create(eq("user-a"), eq(NotificationType.MATCH_RELEASED), eq("match-1"));
        }

        @Test
        @DisplayName("당사자가 아닌 사용자가 취소 시 403 예외")
        void failNotParty() {
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(matchedMatching()));

            assertThatThrownBy(() -> matchingService.cancelMatchedMatching("match-1", "user-x", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("MATCHED가 아닌 상태에서 취소 시 409 예외")
        void failNotMatched() {
            Matching pending = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.PENDING)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            given(matchingRepository.findById("match-1")).willReturn(Optional.of(pending));

            assertThatThrownBy(() -> matchingService.cancelMatchedMatching("match-1", "user-a", null))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.CONFLICT);
        }
    }
}
