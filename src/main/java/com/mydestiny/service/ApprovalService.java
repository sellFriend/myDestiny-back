package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Invitation;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.dto.profile.ProfileUpdateRequest;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.InvitationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ApprovalService {

    private final DatingProfileRepository profileRepository;
    private final InvitationRepository invitationRepository;
    private final InvitationService invitationService;

    @Transactional(readOnly = true)
    public ProfileDetailResponse getProfileForSubject(String token, String userId) {
        DatingProfile profile = resolveSubjectProfile(token, userId);
        return ProfileDetailResponse.from(profile, null);
    }

    @Transactional
    public ProfileDetailResponse updateProfileAsSubject(String token, String userId,
                                                        ProfileUpdateRequest req) {
        DatingProfile profile = resolveSubjectProfile(token, userId);

        if (profile.getStatus() != ProfileStatus.PENDING_APPROVAL
                && profile.getStatus() != ProfileStatus.REVIEW_REQUIRED) {
            throw new BusinessException("확인 대기 상태에서만 수정할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        profile.updateBySubject(req.name(), req.age(), req.gender(), req.isStudent(),
                req.schoolName(), req.major(), req.occupation(),
                req.mbti(), req.hobby(), req.introduction(),
                req.kakaoId(), req.instagramId());
        profileRepository.save(profile);
        return ProfileDetailResponse.from(profile, null);
    }

    @Transactional
    public void recordConsent(String token, String userId) {
        DatingProfile profile = resolveSubjectProfile(token, userId);
        profile.recordConsent();
        profileRepository.save(profile);
    }

    @Transactional
    public ProfileDetailResponse approve(String token, String userId) {
        DatingProfile profile = resolveSubjectProfile(token, userId);

        if (profile.getConsentAgreedAt() == null) {
            throw new BusinessException("개인정보 동의 후 승인할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        profile.approve();
        profileRepository.save(profile);

        markInvitationUsed(token);
        return ProfileDetailResponse.from(profile, null);
    }

    @Transactional
    public void reject(String token, String userId, String reason) {
        DatingProfile profile = resolveSubjectProfile(token, userId);
        profile.reject(reason);
        profileRepository.save(profile);
        markInvitationUsed(token);
    }

    private DatingProfile resolveSubjectProfile(String token, String userId) {
        Invitation invitation = invitationService.findValidInvitation(token);
        DatingProfile profile = invitation.getProfile();

        if (!profile.isSubject(userId)) {
            throw new BusinessException("승인 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return profile;
    }

    private void markInvitationUsed(String token) {
        invitationRepository.findByTokenHash(com.mydestiny.util.TokenUtil.sha256(token))
                .ifPresent(inv -> {
                    inv.markUsed();
                    invitationRepository.save(inv);
                });
    }
}
