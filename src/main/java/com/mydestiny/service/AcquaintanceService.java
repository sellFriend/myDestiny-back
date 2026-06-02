package com.mydestiny.service;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.AcquaintancePhoto;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.Gender;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.domain.enums.RegistrationStatus;
import com.mydestiny.dto.acquaintance.AcquaintanceDetailResponse;
import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.AcquaintancePhotoRepository;
import com.mydestiny.repository.AcquaintanceRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AcquaintanceService {

    private final AcquaintanceRepository acquaintanceRepository;
    private final AcquaintancePhotoRepository acquaintancePhotoRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final NotificationService notificationService;

    @Value("${app.form.base-url:http://localhost:3000/form}")
    private String formBaseUrl;

    // 마담 자신의 영구 폼 링크 반환
    @Transactional(readOnly = true)
    public InviteResponse getFormLink(String userId) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        return new InviteResponse(formBaseUrl + "/" + userId);
    }

    // 친구가 폼 링크 열 때 마담 존재 여부 확인
    @Transactional(readOnly = true)
    public void validateToken(String madamId) {
        if (!userRepository.existsById(madamId)) {
            throw new BusinessException("유효하지 않은 폼 링크입니다.", HttpStatus.NOT_FOUND);
        }
    }

    // 친구(매물)가 카카오 로그인 후 프로필 제출
    @Transactional
    public FormDataResponse submitForm(String madamId, String friendUserId, FormDataRequest req) {
        if (madamId.equals(friendUserId)) {
            throw new BusinessException("본인을 매물로 등록할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        User madam = userRepository.findById(madamId)
                .orElseThrow(() -> new BusinessException("유효하지 않은 폼 링크입니다.", HttpStatus.NOT_FOUND));

        // 전화번호 기준 중복 체크 — VERIFIED 상태인 경우만 차단
        if (acquaintanceRepository.existsByPhoneNumberAndRegistrationStatus(
                req.phoneNumber(), RegistrationStatus.VERIFIED)) {
            throw new BusinessException("이미 다른 마담을 통해 등록 완료된 번호입니다.", HttpStatus.CONFLICT);
        }

        User friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Gender gender = req.gender() != null ? Gender.fromDb(req.gender()) : null;
        String uploadToken = UUID.randomUUID().toString().replace("-", "");

        Acquaintance acquaintance = acquaintanceRepository.save(Acquaintance.builder()
                .user(madam)
                .name(req.name())
                .age(req.age())
                .gender(gender)
                .job(req.job())
                .intro(req.intro())
                .mbti(req.mbti())
                .hobbies(req.hobbies())
                .phoneNumber(req.phoneNumber())
                .email(friend.getEmail())
                .kakaoId(req.kakaoId())
                .instagramId(req.instagramId())
                .verificationToken(uploadToken)
                .tokenExpiresAt(null)
                .build());

        // 카카오 프로필 사진 사용 허용 시 첫 번째 사진으로 등록
        if (req.useKakaoPhoto() && friend.getKakaoProfileImageUrl() != null) {
            acquaintancePhotoRepository.save(AcquaintancePhoto.builder()
                    .acquaintance(acquaintance)
                    .imageUrl(friend.getKakaoProfileImageUrl())
                    .displayOrder(0)
                    .build());
        }

        return new FormDataResponse(acquaintance.getId(), uploadToken, acquaintance.getRegistrationStatus().getDbValue());
    }

    // 폼 제출 후 사진 추가 업로드 — submitForm 응답의 uploadToken 사용
    @Transactional
    public String uploadPhoto(String uploadToken, MultipartFile file) {
        Acquaintance acquaintance = acquaintanceRepository.findByVerificationToken(uploadToken)
                .orElseThrow(() -> new BusinessException("유효하지 않은 업로드 토큰입니다.", HttpStatus.NOT_FOUND));

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
    public List<AcquaintanceDetailResponse> getMyAcquaintances(String userId) {
        return acquaintanceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(AcquaintanceDetailResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public AcquaintanceDetailResponse getAcquaintance(String acquaintanceId, String userId) {
        return AcquaintanceDetailResponse.from(findOwnedAcquaintance(acquaintanceId, userId));
    }

    @Transactional
    public void approve(String acquaintanceId, String userId) {
        Acquaintance acquaintance = findOwnedAcquaintance(acquaintanceId, userId);
        acquaintance.approve();
        acquaintanceRepository.save(acquaintance);
        notificationService.create(userId, NotificationType.VERIFICATION_DONE, acquaintanceId);
    }

    @Transactional
    public void reject(String acquaintanceId, String userId) {
        Acquaintance acquaintance = findOwnedAcquaintance(acquaintanceId, userId);
        acquaintance.reject();
        acquaintanceRepository.save(acquaintance);
    }

    private Acquaintance findOwnedAcquaintance(String acquaintanceId, String userId) {
        Acquaintance acquaintance = acquaintanceRepository.findById(acquaintanceId)
                .orElseThrow(() -> new BusinessException("등록된 친구를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!acquaintance.getUser().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return acquaintance;
    }
}
