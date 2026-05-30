package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.MatchCandidateConsent;
import com.mydestiny.domain.Matching;
import com.mydestiny.domain.MatchLog;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ConsentStatus;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.Role;
import com.mydestiny.dto.matching.ConsentApproveResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.MatchCandidateConsentRepository;
import com.mydestiny.repository.MatchLogRepository;
import com.mydestiny.repository.MatchingRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CandidateConsentServiceTest {

    @Mock MatchCandidateConsentRepository consentRepository;
    @Mock MatchingRepository matchingRepository;
    @Mock MatchLogRepository matchLogRepository;
    @Mock NotificationService notificationService;

    @InjectMocks CandidateConsentService consentService;

    private User requester;
    private User receiver;
    private User candidateB;
    private User candidateD;
    private DatingProfile requesterProfile;
    private DatingProfile targetProfile;
    private Matching matching;
    private MatchCandidateConsent consentB;
    private MatchCandidateConsent consentD;

    @BeforeEach
    void setUp() {
        requester = User.builder().id("user-a").kakaoId("k-a").email("a@t.com").nickname("A").role(Role.USER).build();
        receiver  = User.builder().id("user-c").kakaoId("k-c").email("c@t.com").nickname("C").role(Role.USER).build();
        candidateB = User.builder().id("user-b").kakaoId("k-b").email("b@t.com").nickname("B").role(Role.USER).build();
        candidateD = User.builder().id("user-d").kakaoId("k-d").email("d@t.com").nickname("D").role(Role.USER).build();

        requesterProfile = DatingProfile.builder()
                .id("profile-b").registrant(requester).subject(candidateB)
                .status(ProfileStatus.PUBLISHED).name("B").subjectPhoneHash("hash-b").build();
        targetProfile = DatingProfile.builder()
                .id("profile-d").registrant(receiver).subject(candidateD)
                .status(ProfileStatus.PUBLISHED).name("D").subjectPhoneHash("hash-d").build();

        matching = Matching.builder()
                .id("match-1").requester(requester).receiver(receiver)
                .requesterProfile(requesterProfile).targetProfile(targetProfile)
                .status(MatchingStatus.CONSENT_PENDING)
                .receiverExpiresAt(LocalDateTime.now().plusHours(72))
                .build();

        consentB = MatchCandidateConsent.builder()
                .id("consent-b").matching(matching).profile(requesterProfile)
                .ownerUser(candidateB).status(ConsentStatus.PENDING)
                .consentToken("token-b").expiresAt(LocalDateTime.now().plusHours(48))
                .build();

        consentD = MatchCandidateConsent.builder()
                .id("consent-d").matching(matching).profile(targetProfile)
                .ownerUser(candidateD).status(ConsentStatus.PENDING)
                .consentToken("token-d").expiresAt(LocalDateTime.now().plusHours(48))
                .build();
    }

    // ────── approveConsent ──────

    @Nested
    @DisplayName("approveConsent")
    class ApproveConsent {

        @Test
        @DisplayName("첫 번째 동의 시 CONSENT_PARTIALLY_APPROVED 로 전이")
        void firstApproval() {
            given(consentRepository.findById("consent-b")).willReturn(Optional.of(consentB));
            given(consentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(consentRepository.countByMatchingIdAndStatus("match-1", ConsentStatus.APPROVED)).willReturn(1L);
            given(consentRepository.findByMatchingId("match-1")).willReturn(List.of(consentB, consentD));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ConsentApproveResponse result = consentService.approveConsent("consent-b", "user-b");

            assertThat(result.consentStatus()).isEqualTo(ConsentStatus.APPROVED.name());
            assertThat(result.matchingStatus()).isEqualTo(MatchingStatus.CONSENT_PARTIALLY_APPROVED.name());
            // 상대 후보(D)에게 counterpart 알림
            verify(notificationService).create(eq("user-d"), any(), anyString());
        }

        @Test
        @DisplayName("두 번째 동의 시 MATCHED 로 전이 + 전원 알림")
        void secondApproval_matched() {
            // consentD는 이미 APPROVED 상태 (두 번째 동의자 시뮬레이션)
            MatchCandidateConsent approvedD = MatchCandidateConsent.builder()
                    .id("consent-d").matching(matching).profile(targetProfile)
                    .ownerUser(candidateD).status(ConsentStatus.APPROVED)
                    .consentToken("token-d").expiresAt(LocalDateTime.now().plusHours(48))
                    .build();

            // matching이 CONSENT_PARTIALLY_APPROVED 상태
            Matching partialMatching = Matching.builder()
                    .id("match-1").requester(requester).receiver(receiver)
                    .requesterProfile(requesterProfile).targetProfile(targetProfile)
                    .status(MatchingStatus.CONSENT_PARTIALLY_APPROVED)
                    .receiverExpiresAt(LocalDateTime.now().plusHours(72))
                    .build();

            MatchCandidateConsent pendingB = MatchCandidateConsent.builder()
                    .id("consent-b").matching(partialMatching).profile(requesterProfile)
                    .ownerUser(candidateB).status(ConsentStatus.PENDING)
                    .consentToken("token-b").expiresAt(LocalDateTime.now().plusHours(48))
                    .build();

            given(consentRepository.findById("consent-b")).willReturn(Optional.of(pendingB));
            given(consentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(consentRepository.countByMatchingIdAndStatus("match-1", ConsentStatus.APPROVED)).willReturn(2L);
            given(consentRepository.findByMatchingId("match-1")).willReturn(List.of(pendingB, approvedD));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            ConsentApproveResponse result = consentService.approveConsent("consent-b", "user-b");

            assertThat(result.matchingStatus()).isEqualTo(MatchingStatus.MATCHED.name());
            // A, C, B, D 4명에게 MATCHED 알림 (requester + receiver + consent 소유자 2명)
            verify(notificationService).create(eq("user-a"), any(), anyString());
            verify(notificationService).create(eq("user-c"), any(), anyString());
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 동의 시 403 예외")
        void failNotOwner() {
            given(consentRepository.findById("consent-b")).willReturn(Optional.of(consentB));

            assertThatThrownBy(() -> consentService.approveConsent("consent-b", "user-x"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }

        @Test
        @DisplayName("이미 동의된 consent에 재동의 시 예외")
        void failAlreadyApproved() {
            MatchCandidateConsent alreadyApproved = MatchCandidateConsent.builder()
                    .id("consent-b").matching(matching).profile(requesterProfile)
                    .ownerUser(candidateB).status(ConsentStatus.APPROVED)
                    .consentToken("token-b").expiresAt(LocalDateTime.now().plusHours(48))
                    .build();
            given(consentRepository.findById("consent-b")).willReturn(Optional.of(alreadyApproved));

            assertThatThrownBy(() -> consentService.approveConsent("consent-b", "user-b"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    // ────── rejectConsent ──────

    @Nested
    @DisplayName("rejectConsent")
    class RejectConsent {

        @Test
        @DisplayName("당사자 거절 시 CONSENT_REJECTED 로 전이 + 전원 알림")
        void success() {
            given(consentRepository.findById("consent-b")).willReturn(Optional.of(consentB));
            given(consentRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(consentRepository.findByMatchingId("match-1")).willReturn(List.of(consentB, consentD));
            given(matchingRepository.save(any())).willAnswer(inv -> inv.getArgument(0));
            given(matchLogRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

            consentService.rejectConsent("consent-b", "user-b");

            assertThat(matching.getStatus()).isEqualTo(MatchingStatus.CONSENT_REJECTED);
            // 요청자(A), 수신자(C), 상대 후보(D)에게 실패 알림
            verify(notificationService).create(eq("user-a"), any(), anyString());
            verify(notificationService).create(eq("user-c"), any(), anyString());
            verify(notificationService).create(eq("user-d"), any(), anyString());
        }

        @Test
        @DisplayName("소유자가 아닌 사용자가 거절 시 403 예외")
        void failNotOwner() {
            given(consentRepository.findById("consent-b")).willReturn(Optional.of(consentB));

            assertThatThrownBy(() -> consentService.rejectConsent("consent-b", "user-x"))
                    .isInstanceOf(BusinessException.class)
                    .extracting("status").isEqualTo(HttpStatus.FORBIDDEN);
        }
    }
}
