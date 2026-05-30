package com.mydestiny.service;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;
import com.mydestiny.domain.PendingInvite;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.Gender;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.dto.acquaintance.AcquaintanceDetailResponse;
import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.AcquaintancePhotoRepository;
import com.mydestiny.repository.AcquaintanceRepository;
import com.mydestiny.repository.PendingInviteRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcquaintanceService {

    private final PendingInviteRepository pendingInviteRepository;
    private final AcquaintanceRepository acquaintanceRepository;
    private final AcquaintancePhotoRepository acquaintancePhotoRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final NotificationService notificationService;

    @Value("${app.form.base-url:http://localhost:3000/form}")
    private String formBaseUrl;

    @Transactional
    public InviteResponse createInvite(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        String token = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime expiresAt = LocalDateTime.now().plusDays(7);

        pendingInviteRepository.save(PendingInvite.builder()
                .user(user)
                .token(token)
                .expiresAt(expiresAt)
                .build());

        return new InviteResponse(formBaseUrl + "/" + token, expiresAt);
    }

    @Transactional(readOnly = true)
    public void validateToken(String token) {
        PendingInvite invite = pendingInviteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("유효하지 않은 초대 링크입니다.", HttpStatus.NOT_FOUND));
        if (invite.isExpired()) {
            throw new BusinessException("만료된 초대 링크입니다.", HttpStatus.GONE);
        }
    }

    @Transactional
    public FormDataResponse submitForm(String token, FormDataRequest req) {
        PendingInvite invite = pendingInviteRepository.findByToken(token)
                .orElseThrow(() -> new BusinessException("유효하지 않은 초대 링크입니다.", HttpStatus.NOT_FOUND));
        if (invite.isExpired()) {
            throw new BusinessException("만료된 초대 링크입니다.", HttpStatus.GONE);
        }

        Gender gender = req.gender() != null ? Gender.fromDb(req.gender()) : null;

        Acquaintance acquaintance = acquaintanceRepository.save(Acquaintance.builder()
                .user(invite.getUser())
                .name(req.name())
                .age(req.age())
                .gender(gender)
                .job(req.job())
                .intro(req.intro())
                .mbti(req.mbti())
                .hobbies(req.hobbies())
                .phoneNumber(req.phoneNumber())
                .email(req.email())
                .kakaoId(req.kakaoId())
                .instagramId(req.instagramId())
                .verificationToken(token)
                .tokenExpiresAt(invite.getExpiresAt())
                .build());

        pendingInviteRepository.delete(invite);

        return new FormDataResponse(acquaintance.getId(), acquaintance.getRegistrationStatus().getDbValue());
    }

    @Transactional
    public String uploadPhoto(String token, MultipartFile file) {
        Acquaintance acquaintance = acquaintanceRepository.findByVerificationToken(token)
                .orElseThrow(() -> new BusinessException("유효하지 않은 토큰입니다.", HttpStatus.NOT_FOUND));

        int count = acquaintancePhotoRepository.countByAcquaintanceId(acquaintance.getId());
        if (count >= 5) {
            throw new BusinessException("사진은 최대 5장까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String url = objectStorageService.upload(file, "acquaintances/" + acquaintance.getId());

        acquaintancePhotoRepository.save(AcquaintancePhoto.builder()
                .acquaintance(acquaintance)
                .imageUrl(url)
                .displayOrder(count)
                .build());

        return url;
    }

    @Transactional(readOnly = true)
    public AcquaintanceDetailResponse getAcquaintance(String acquaintanceId, String userId) {
        Acquaintance acquaintance = acquaintanceRepository.findById(acquaintanceId)
                .orElseThrow(() -> new BusinessException("등록된 친구를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!acquaintance.getUser().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return AcquaintanceDetailResponse.from(acquaintance);
    }

    @Transactional
    public void approve(String acquaintanceId, String userId) {
        Acquaintance acquaintance = acquaintanceRepository.findById(acquaintanceId)
                .orElseThrow(() -> new BusinessException("등록된 친구를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!acquaintance.getUser().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        acquaintance.approve();
        acquaintanceRepository.save(acquaintance);
        notificationService.create(userId, NotificationType.VERIFICATION_DONE, acquaintanceId);
    }

    @Transactional
    public void reject(String acquaintanceId, String userId) {
        Acquaintance acquaintance = acquaintanceRepository.findById(acquaintanceId)
                .orElseThrow(() -> new BusinessException("등록된 친구를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!acquaintance.getUser().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        acquaintance.reject();
        acquaintanceRepository.save(acquaintance);
    }
}
