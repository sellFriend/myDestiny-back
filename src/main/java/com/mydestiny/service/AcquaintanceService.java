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
import com.mydestiny.dto.acquaintance.FormPrefillResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import com.mydestiny.dto.acquaintance.PhotoResponse;
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
import java.util.Set;
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

    // 친구가 폼 진입 시 — 마담 존재 검증 + 본인의 기존 작성분 있으면 prefill 데이터 반환
    @Transactional(readOnly = true)
    public FormPrefillResponse getFormState(String madamId, String friendUserId) {
        if (!userRepository.existsById(madamId)) {
            throw new BusinessException("유효하지 않은 폼 링크입니다.", HttpStatus.NOT_FOUND);
        }
        if (friendUserId == null || friendUserId.equals(madamId)) {
            return FormPrefillResponse.empty();
        }
        return acquaintanceRepository
                .findByUserIdAndFriendUserIdAndDeletedAtIsNull(madamId, friendUserId)
                .map(FormPrefillResponse::of)
                .orElseGet(FormPrefillResponse::empty);
    }

    // 친구(매물)가 카카오 로그인 후 프로필 제출 또는 수정 재제출
    @Transactional
    public FormDataResponse submitForm(String madamId, String friendUserId, FormDataRequest req) {
        if (madamId.equals(friendUserId)) {
            throw new BusinessException("본인을 매물로 등록할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        User madam = userRepository.findById(madamId)
                .orElseThrow(() -> new BusinessException("유효하지 않은 폼 링크입니다.", HttpStatus.NOT_FOUND));

        User friend = userRepository.findById(friendUserId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        Gender gender = req.gender() != null ? Gender.fromDb(req.gender()) : null;

        // 기존 작성분이 있으면 update — 없으면 신규 create
        Acquaintance existing = acquaintanceRepository
                .findByUserIdAndFriendUserIdAndDeletedAtIsNull(madamId, friendUserId)
                .orElse(null);

        if (existing != null) {
            if (existing.getRegistrationStatus() == RegistrationStatus.VERIFIED) {
                throw new BusinessException("이미 등록 완료된 카드입니다.", HttpStatus.CONFLICT);
            }
            existing.updateForResubmit(
                    req.name(), req.age(), gender, req.job(), req.intro(),
                    req.mbti(), req.hobbies(), req.phoneNumber(), friend.getEmail(),
                    req.kakaoId(), req.instagramId()
            );
            acquaintanceRepository.save(existing);
            notificationService.create(madamId, NotificationType.FORM_SUBMITTED, existing.getId());
            return new FormDataResponse(existing.getId(), existing.getVerificationToken(), existing.getRegistrationStatus().getDbValue());
        }

        // 신규 생성 시에만 전화번호 중복 체크 (다른 마담을 통해 이미 VERIFIED인 번호 차단)
        if (acquaintanceRepository.existsByPhoneNumberAndRegistrationStatus(
                req.phoneNumber(), RegistrationStatus.VERIFIED)) {
            throw new BusinessException("이미 다른 마담을 통해 등록 완료된 번호입니다.", HttpStatus.CONFLICT);
        }

        String uploadToken = UUID.randomUUID().toString().replace("-", "");

        Acquaintance acquaintance = acquaintanceRepository.save(Acquaintance.builder()
                .user(madam)
                .friendUser(friend)
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

        notificationService.create(madamId, NotificationType.FORM_SUBMITTED, acquaintance.getId());

        return new FormDataResponse(acquaintance.getId(), uploadToken, acquaintance.getRegistrationStatus().getDbValue());
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    // 폼 사진 목록 조회 — uploadToken 으로 현재 등록된 사진과 photoId 확인
    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotos(String uploadToken) {
        Acquaintance acquaintance = findAcquaintanceByToken(uploadToken);
        return acquaintancePhotoRepository.findByAcquaintanceIdOrderByDisplayOrder(acquaintance.getId()).stream()
                .map(PhotoResponse::from)
                .toList();
    }

    // 폼 제출 후 사진 추가 업로드 — submitForm 응답의 uploadToken 사용
    @Transactional
    public PhotoResponse uploadPhoto(String uploadToken, MultipartFile file) {
        Acquaintance acquaintance = findAcquaintanceByToken(uploadToken);
        validateImageType(file);

        int count = acquaintancePhotoRepository.countByAcquaintanceId(acquaintance.getId());
        if (count >= 5) {
            throw new BusinessException("사진은 최대 5장까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String url = objectStorageService.upload(file, "acquaintances/" + acquaintance.getId());

        AcquaintancePhoto photo = acquaintancePhotoRepository.save(AcquaintancePhoto.builder()
                .acquaintance(acquaintance)
                .imageUrl(url)
                .displayOrder(count)
                .build());

        return PhotoResponse.from(photo);
    }

    // 폼 사진 교체 — 기존 파일을 스토리지에서 삭제하고 새 파일로 대체
    @Transactional
    public PhotoResponse replacePhoto(String uploadToken, String photoId, MultipartFile file) {
        Acquaintance acquaintance = findAcquaintanceByToken(uploadToken);
        validateImageType(file);

        AcquaintancePhoto photo = acquaintancePhotoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException("사진을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!photo.getAcquaintance().getId().equals(acquaintance.getId())) {
            throw new BusinessException("해당 사진에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        objectStorageService.delete(photo.getImageUrl());
        String url = objectStorageService.upload(file, "acquaintances/" + acquaintance.getId());
        photo.changeImageUrl(url);

        return PhotoResponse.from(acquaintancePhotoRepository.save(photo));
    }

    private Acquaintance findAcquaintanceByToken(String uploadToken) {
        return acquaintanceRepository.findByVerificationToken(uploadToken)
                .orElseThrow(() -> new BusinessException("유효하지 않은 업로드 토큰입니다.", HttpStatus.NOT_FOUND));
    }

    private void validateImageType(MultipartFile file) {
        if (file == null || file.isEmpty() || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException("허용되지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public List<AcquaintanceDetailResponse> getMyAcquaintances(String userId) {
        return acquaintanceRepository.findByUserIdAndDeletedAtIsNullOrderByCreatedAtDesc(userId).stream()
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

    @Transactional
    public void requestEdit(String acquaintanceId, String userId) {
        Acquaintance acquaintance = findOwnedAcquaintance(acquaintanceId, userId);
        try {
            acquaintance.requestEdit();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.CONFLICT);
        }
        acquaintanceRepository.save(acquaintance);
        if (acquaintance.getFriendUser() != null) {
            notificationService.create(
                    acquaintance.getFriendUser().getId(),
                    NotificationType.EDIT_REQUESTED,
                    acquaintanceId
            );
        }
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
