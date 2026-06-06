package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.acquaintance.AcquaintanceDetailResponse;
import com.mydestiny.dto.acquaintance.FormDataRequest;
import com.mydestiny.dto.acquaintance.FormDataResponse;
import com.mydestiny.dto.acquaintance.FormPrefillResponse;
import com.mydestiny.dto.acquaintance.InviteResponse;
import com.mydestiny.dto.acquaintance.PhotoResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.MatchingRepository;
import com.mydestiny.repository.ProfilePhotoRepository;
import com.mydestiny.repository.UserRepository;
import com.mydestiny.util.PhoneEncryptionUtil;
import com.mydestiny.util.PhoneHashUtil;
import com.mydestiny.util.PhoneLookupUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * 주선자 폼 흐름 — 친구(subject)가 본인 정보를 작성하고 주선자(registrant)가 승인한다.
 * 저장소는 dating_profiles 단일 시스템을 사용한다. (구 acquaintances 테이블 통합)
 */
@Service
@RequiredArgsConstructor
public class AcquaintanceService {

    private final DatingProfileRepository profileRepository;
    private final ProfilePhotoRepository profilePhotoRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final NotificationService notificationService;
    private final PhoneHashUtil phoneHashUtil;
    private final PhoneEncryptionUtil phoneEncryptionUtil;
    private final PhoneLookupUtil phoneLookupUtil;
    private final MatchingRepository matchingRepository;

    // 수정 요청 자체가 불가한 상태 — 상대가 수락했거나 동의 진행 중/성사된 매칭(사실상 매칭됨)
    private static final List<MatchingStatus> MATCH_COMMITTED_STATUSES = List.of(
            MatchingStatus.ACCEPTED_BY_RECEIVER,
            MatchingStatus.CONSENT_PENDING,
            MatchingStatus.CONSENT_PARTIALLY_APPROVED,
            MatchingStatus.MATCHED
    );

    @Value("${app.form.base-url:http://localhost:3000/form}")
    private String formBaseUrl;

    @Value("${app.cors.allowed-origins:http://localhost:3000,http://localhost:5173}")
    private List<String> allowedOrigins;

    // 마담 자신의 영구 폼 링크 반환
    // 매물(친구)로 등록된 사용자는 주선자 역할 불가
    @Transactional(readOnly = true)
    public InviteResponse getFormLink(String userId, String requestOrigin) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (profileRepository.existsBySubjectIdAndStatusNot(userId, ProfileStatus.DELETED)) {
            throw new BusinessException("매물로 등록된 사용자는 주선자 역할을 할 수 없습니다.", HttpStatus.FORBIDDEN);
        }
        return new InviteResponse(resolveFormBaseUrl(requestOrigin) + "/" + userId);
    }

    private String resolveFormBaseUrl(String requestOrigin) {
        if (requestOrigin != null && !requestOrigin.isBlank()
                && allowedOrigins.contains(requestOrigin)) {
            return requestOrigin + "/form";
        }
        return formBaseUrl;
    }

    // 친구가 폼 진입 시 — 마담 존재 검증 + 본인의 기존 작성분 있으면 prefill 데이터 반환
    // 차단: 본인 폼, 이미 주선자인 사람, 이미 다른 마담의 친구로 등록된 사람
    @Transactional(readOnly = true)
    public FormPrefillResponse getFormState(String madamId, String friendUserId) {
        if (!userRepository.existsById(madamId)) {
            throw new BusinessException("유효하지 않은 폼 링크입니다.", HttpStatus.NOT_FOUND);
        }
        if (madamId.equals(friendUserId)) {
            throw new BusinessException("본인의 폼에는 등록할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (friendUserId == null) {
            return FormPrefillResponse.empty();
        }
        if (profileRepository.existsByRegistrantIdAndStatusNot(friendUserId, ProfileStatus.DELETED)) {
            throw new BusinessException("주선자는 친구로 등록될 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        DatingProfile existing = profileRepository
                .findByRegistrantIdAndSubjectIdAndStatusNot(madamId, friendUserId, ProfileStatus.DELETED)
                .orElse(null);
        if (existing != null) {
            String phone = existing.getSubjectPhoneEncrypted() != null
                    ? phoneEncryptionUtil.decrypt(existing.getSubjectPhoneEncrypted())
                    : null;
            return FormPrefillResponse.of(existing, phone);
        }
        if (profileRepository.existsBySubjectIdAndStatusNot(friendUserId, ProfileStatus.DELETED)) {
            throw new BusinessException("이미 다른 주선자를 통해 등록되어 있습니다.", HttpStatus.BAD_REQUEST);
        }
        return FormPrefillResponse.empty();
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

        if (profileRepository.existsByRegistrantIdAndStatusNot(friendUserId, ProfileStatus.DELETED)) {
            throw new BusinessException("주선자는 친구로 등록될 수 없습니다.", HttpStatus.BAD_REQUEST);
        }

        String phoneHash = phoneHashUtil.hash(req.phoneNumber());
        String phoneEncrypted = phoneEncryptionUtil.encrypt(req.phoneNumber());
        String phoneLookup = phoneLookupUtil.lookup(req.phoneNumber());

        // 기존 작성분이 있으면 update — 없으면 신규 create
        DatingProfile existing = profileRepository
                .findByRegistrantIdAndSubjectIdAndStatusNot(madamId, friendUserId, ProfileStatus.DELETED)
                .orElse(null);

        if (existing != null) {
            // 수정 요청을 받은(DRAFT) 카드 + 아직 승인 전(PENDING_APPROVAL) 카드는 친구가 직접 재제출 가능.
            // PUBLISHED 등 그 외 상태는 주선자의 수정 요청을 거쳐야 함
            if (existing.getStatus() != ProfileStatus.DRAFT
                    && existing.getStatus() != ProfileStatus.PENDING_APPROVAL) {
                throw new BusinessException(
                        "주선자에게 폼 수정 요청을 받은 카드만 수정할 수 있습니다.",
                        HttpStatus.CONFLICT);
            }

            existing.resubmitByFriend(
                    req.name(), req.age(), req.gender(), req.job(), req.intro(),
                    req.mbti(), req.hobbies(), req.kakaoId(), req.instagramId(),
                    phoneHash, phoneEncrypted, phoneLookup
            );
            profileRepository.save(existing);
            notificationService.create(madamId, NotificationType.FORM_SUBMITTED, existing.getId());
            return new FormDataResponse(existing.getId(), existing.getUploadToken(), existing.getStatus().name());
        }

        // 신규 생성 시 — 다른 마담의 친구로 이미 등록됐는지 차단 (계정 기준)
        if (profileRepository.existsBySubjectIdAndStatusNot(friendUserId, ProfileStatus.DELETED)) {
            throw new BusinessException("이미 다른 주선자를 통해 등록되어 있습니다.", HttpStatus.CONFLICT);
        }

        // 같은 전화번호로 이미 승인 완료된 프로필이 있는지 차단 (번호 기준)
        if (profileRepository.existsBySubjectPhoneLookupAndStatus(phoneLookup, ProfileStatus.PUBLISHED)) {
            throw new BusinessException("이미 다른 주선자를 통해 등록 완료된 번호입니다.", HttpStatus.CONFLICT);
        }

        String uploadToken = UUID.randomUUID().toString().replace("-", "");

        DatingProfile profile = profileRepository.save(DatingProfile.builder()
                .registrant(madam)
                .subject(friend)
                .status(ProfileStatus.PENDING_APPROVAL)
                .name(req.name())
                .age(req.age())
                .gender(req.gender() != null ? com.mydestiny.domain.enums.Gender.fromDb(req.gender()) : null)
                .occupation(req.job())
                .introduction(req.intro())
                .mbti(req.mbti())
                .hobby(req.hobbies())
                .kakaoId(req.kakaoId())
                .instagramId(req.instagramId())
                .subjectPhoneHash(phoneHash)
                .subjectPhoneEncrypted(phoneEncrypted)
                .subjectPhoneLookup(phoneLookup)
                .uploadToken(uploadToken)
                .build());

        // 카카오 프로필 사진 사용 허용 시 첫 번째 사진으로 등록
        if (req.useKakaoPhoto() && friend.getKakaoProfileImageUrl() != null) {
            profilePhotoRepository.save(ProfilePhoto.builder()
                    .profile(profile)
                    .imageUrl(friend.getKakaoProfileImageUrl())
                    .displayOrder(0)
                    .build());
        }

        notificationService.create(madamId, NotificationType.FORM_SUBMITTED, profile.getId());

        return new FormDataResponse(profile.getId(), uploadToken, profile.getStatus().name());
    }

    private static final Set<String> ALLOWED_IMAGE_TYPES =
            Set.of("image/jpeg", "image/png", "image/webp", "image/gif");

    // 친구 1명당 등록 가능한 사진 수. 추후 여러 장 확장 시 이 값만 늘리면 됨.
    private static final int MAX_PHOTOS = 1;

    // 폼 사진 목록 조회 — uploadToken 으로 현재 등록된 사진과 photoId 확인
    @Transactional(readOnly = true)
    public List<PhotoResponse> getPhotos(String uploadToken) {
        DatingProfile profile = findProfileByToken(uploadToken);
        return profilePhotoRepository.findByProfileIdOrderByDisplayOrder(profile.getId()).stream()
                .map(PhotoResponse::from)
                .toList();
    }

    // 폼 제출 후 사진 추가 업로드 — submitForm 응답의 uploadToken 사용
    @Transactional
    public PhotoResponse uploadPhoto(String uploadToken, MultipartFile file) {
        DatingProfile profile = findProfileByToken(uploadToken);
        validateImageType(file);

        int count = profilePhotoRepository.countByProfileId(profile.getId());
        if (count >= MAX_PHOTOS) {
            throw new BusinessException("사진은 최대 " + MAX_PHOTOS + "장까지 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String url = objectStorageService.upload(file, "profiles/" + profile.getId());

        ProfilePhoto photo = profilePhotoRepository.save(ProfilePhoto.builder()
                .profile(profile)
                .imageUrl(url)
                .displayOrder(count)
                .build());

        return PhotoResponse.from(photo);
    }

    // 폼 사진 교체 — 기존 파일을 스토리지에서 삭제하고 새 파일로 대체
    @Transactional
    public PhotoResponse replacePhoto(String uploadToken, String photoId, MultipartFile file) {
        DatingProfile profile = findProfileByToken(uploadToken);
        validateImageType(file);

        ProfilePhoto photo = profilePhotoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException("사진을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!photo.getProfile().getId().equals(profile.getId())) {
            throw new BusinessException("해당 사진에 대한 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }

        objectStorageService.delete(photo.getImageUrl());
        String url = objectStorageService.upload(file, "profiles/" + profile.getId());
        photo.changeImageUrl(url);

        return PhotoResponse.from(profilePhotoRepository.save(photo));
    }

    private DatingProfile findProfileByToken(String uploadToken) {
        return profileRepository.findByUploadToken(uploadToken)
                .orElseThrow(() -> new BusinessException("유효하지 않은 업로드 토큰입니다.", HttpStatus.NOT_FOUND));
    }

    private void validateImageType(MultipartFile file) {
        if (file == null || file.isEmpty() || !ALLOWED_IMAGE_TYPES.contains(file.getContentType())) {
            throw new BusinessException("허용되지 않는 이미지 형식입니다.", HttpStatus.BAD_REQUEST);
        }
    }

    @Transactional(readOnly = true)
    public List<AcquaintanceDetailResponse> getMyAcquaintances(String userId) {
        return profileRepository.findByRegistrantIdAndStatusNotOrderByCreatedAtDesc(userId, ProfileStatus.DELETED)
                .stream()
                .map(AcquaintanceDetailResponse::from)
                .toList();
    }

    @Transactional
    public void approve(String profileId, String userId) {
        DatingProfile profile = findOwnedProfile(profileId, userId);
        try {
            profile.approve();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.CONFLICT);
        }
        profileRepository.save(profile);
        notificationService.create(userId, NotificationType.VERIFICATION_DONE, profileId);
    }

    @Transactional
    public void reject(String profileId, String userId) {
        DatingProfile profile = findOwnedProfile(profileId, userId);
        profile.softDelete();
        profileRepository.save(profile);
    }

    @Transactional
    public void requestEdit(String profileId, String userId) {
        DatingProfile profile = findOwnedProfile(profileId, userId);

        // 매칭 상태 게이트 — 매칭(진행/성사)은 불가, 받은/보낸 요청은 정리 후 가능
        if (matchingRepository.isProfileInActiveMatch(profileId, MATCH_COMMITTED_STATUSES)) {
            throw new BusinessException(
                    "이미 매칭이 진행 중이거나 성사된 프로필은 수정 요청할 수 없습니다.",
                    HttpStatus.CONFLICT);
        }
        if (matchingRepository.existsByTargetProfileIdAndStatus(profileId, MatchingStatus.PENDING)) {
            throw new BusinessException(
                    "받은 매칭 요청을 모두 거절한 뒤 수정 요청이 가능합니다.",
                    HttpStatus.CONFLICT);
        }
        if (matchingRepository.existsByRequesterProfileIdAndStatus(profileId, MatchingStatus.PENDING)) {
            throw new BusinessException(
                    "보낸 매칭 요청을 모두 취소한 뒤 수정 요청이 가능합니다.",
                    HttpStatus.CONFLICT);
        }

        try {
            profile.requestEditByRegistrant();
        } catch (IllegalStateException e) {
            throw new BusinessException(e.getMessage(), HttpStatus.CONFLICT);
        }
        profileRepository.save(profile);
        if (profile.getSubject() != null) {
            notificationService.create(
                    profile.getSubject().getId(),
                    NotificationType.EDIT_REQUESTED,
                    profileId
            );
        }
    }

    private DatingProfile findOwnedProfile(String profileId, String userId) {
        DatingProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException("등록된 친구를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (!profile.getRegistrant().getId().equals(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        return profile;
    }
}
