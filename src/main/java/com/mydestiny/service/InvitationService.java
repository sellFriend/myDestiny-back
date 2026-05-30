package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Invitation;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.invitation.InvitationInfoResponse;
import com.mydestiny.dto.invitation.InviteCreateResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.InvitationRepository;
import com.mydestiny.util.TokenUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class InvitationService {

    private final DatingProfileRepository profileRepository;
    private final InvitationRepository invitationRepository;

    @Value("${app.frontend.base-url:http://localhost:3000}")
    private String frontendBaseUrl;

    @Transactional
    public InviteCreateResponse createInvite(String profileId, String userId) {
        DatingProfile profile = findProfile(profileId);

        if (!profile.isOwnedBy(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        if (profile.getStatus() != ProfileStatus.DRAFT) {
            throw new BusinessException("DRAFT 상태의 프로필만 초대할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String rawToken = TokenUtil.generateRawToken();
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        invitationRepository.save(Invitation.builder()
                .profile(profile)
                .tokenHash(TokenUtil.sha256(rawToken))
                .expiresAt(expiresAt)
                .build());

        profile.changeStatus(ProfileStatus.INVITED);
        profileRepository.save(profile);

        String inviteUrl = frontendBaseUrl + "/invitations/" + rawToken;
        return new InviteCreateResponse(inviteUrl, expiresAt);
    }

    @Transactional(readOnly = true)
    public InvitationInfoResponse getInfo(String rawToken) {
        Invitation invitation = findValidInvitation(rawToken);
        return InvitationInfoResponse.from(invitation.getProfile());
    }

    // PhoneVerificationService에서도 사용
    public Invitation findValidInvitation(String rawToken) {
        Invitation invitation = invitationRepository.findByTokenHash(TokenUtil.sha256(rawToken))
                .orElseThrow(() -> new BusinessException("유효하지 않은 초대 링크입니다.", HttpStatus.NOT_FOUND));

        if (invitation.isExpired()) {
            throw new BusinessException("만료된 초대 링크입니다.", HttpStatus.GONE);
        }
        if (invitation.isUsed()) {
            throw new BusinessException("이미 사용된 초대 링크입니다.", HttpStatus.GONE);
        }
        return invitation;
    }

    private DatingProfile findProfile(String profileId) {
        return profileRepository.findByIdAndStatusNot(profileId, ProfileStatus.DELETED)
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }
}
