package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.ProfilePhoto;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.Gender;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.ProfileVisibility;
import com.mydestiny.dto.profile.ProfileCreateRequest;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.dto.profile.PublicProfileResponse;
import com.mydestiny.dto.profile.ProfileUpdateRequest;
import com.mydestiny.service.FollowService;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.MatchingRepository;
import com.mydestiny.repository.ProfilePhotoRepository;
import com.mydestiny.repository.UserRepository;
import com.mydestiny.util.PhoneEncryptionUtil;
import com.mydestiny.util.PhoneHashUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final DatingProfileRepository profileRepository;
    private final ProfilePhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final ObjectStorageService objectStorageService;
    private final PhoneHashUtil phoneHashUtil;
    private final PhoneEncryptionUtil phoneEncryptionUtil;
    private final com.mydestiny.util.PhoneLookupUtil phoneLookupUtil;
    private final FollowService followService;
    private final MatchingRepository matchingRepository;

    @Transactional
    public ProfileDetailResponse create(String userId, ProfileCreateRequest req) {
        User registrant = findUser(userId);

        validateStudentFields(req.isStudent(), req.schoolName(), req.major(), req.occupation());

        String encryptedPhone = phoneEncryptionUtil.encrypt(req.subjectPhone());

        DatingProfile profile = profileRepository.save(
                DatingProfile.builder()
                        .registrant(registrant)
                        .name(req.name())
                        .age(req.age())
                        .gender(req.gender() != null ? Gender.fromDb(req.gender()) : null)
                        .isStudent(req.isStudent())
                        .schoolName(req.schoolName())
                        .major(req.major())
                        .occupation(req.occupation())
                        .mbti(req.mbti())
                        .hobby(req.hobby())
                        .introduction(req.introduction())
                        .kakaoId(req.kakaoId())
                        .instagramId(req.instagramId())
                        .subjectPhoneHash(phoneHashUtil.hash(req.subjectPhone()))
                        .subjectPhoneEncrypted(encryptedPhone)
                        .subjectPhoneLookup(phoneLookupUtil.lookup(req.subjectPhone()))
                        .build()
        );
        return ProfileDetailResponse.from(profile, req.subjectPhone());
    }

    @Transactional(readOnly = true)
    public List<PublicProfileResponse> getPublicProfiles(String currentUserId, String registrantId, String gender) {
        var mutualFollowIds = followService.getMutualFollowIds(currentUserId);
        Gender genderFilter = gender != null ? Gender.fromDb(gender) : null;

        List<PublicProfileResponse> profiles = profileRepository.findPublishedExcludingOccupied(ProfileStatus.PUBLISHED, MatchingStatus.OCCUPIED)
                .stream()
                .filter(p -> registrantId != null
                        ? p.getRegistrant().getId().equals(registrantId)
                        : !p.getRegistrant().getId().equals(currentUserId))
                .filter(p -> genderFilter == null || p.getGender() == genderFilter)
                .filter(p -> switch (p.getVisibility()) {
                    case PUBLIC -> true;
                    case FOLLOWERS_ONLY -> mutualFollowIds.contains(p.getRegistrant().getId());
                    case PRIVATE -> false;
                })
                .map(PublicProfileResponse::from)
                .collect(java.util.stream.Collectors.toCollection(java.util.ArrayList::new));
        java.util.Collections.shuffle(profiles);
        return profiles;
    }

    @Transactional
    public void changeVisibility(String profileId, String userId, ProfileVisibility visibility) {
        DatingProfile profile = findActive(profileId);
        checkOwner(profile, userId);
        profile.changeVisibility(visibility);
        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public ProfileDetailResponse getDetail(String profileId, String userId) {
        DatingProfile profile = findActive(profileId);
        if (profile.getStatus() != ProfileStatus.PUBLISHED) {
            checkOwner(profile, userId);
        }
        boolean matched = isMatched(profileId);
        if (profile.isOwnedBy(userId)) {
            return ProfileDetailResponse.from(profile, decryptSubjectPhone(profile), matched);
        } else if (profile.isSubject(userId)) {
            return ProfileDetailResponse.from(profile, null, matched);
        } else {
            return ProfileDetailResponse.fromPublic(profile, matched);
        }
    }

    @Transactional
    public ProfileDetailResponse update(String profileId, String userId, ProfileUpdateRequest req) {
        DatingProfile profile = findActive(profileId);
        checkOwner(profile, userId);

        if (req.isStudent() != null) {
            validateStudentFields(req.isStudent(), req.schoolName(), req.major(), req.occupation());
        }

        profile.updateByRegistrant(req.name(), req.age(), req.gender(), req.isStudent(),
                req.schoolName(), req.major(), req.occupation(),
                req.mbti(), req.hobby(), req.introduction(),
                req.kakaoId(), req.instagramId());
        profileRepository.save(profile);
        return ProfileDetailResponse.from(profile, decryptSubjectPhone(profile), isMatched(profileId));
    }

    @Transactional
    public void delete(String profileId, String userId) {
        DatingProfile profile = findActive(profileId);
        // 등록자(A) 또는 당사자(B) 모두 삭제 가능
        if (!profile.isOwnedBy(userId) && !profile.isSubject(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
        profile.softDelete();
        profileRepository.save(profile);
    }

    @Transactional
    public String uploadPhoto(String profileId, String userId, MultipartFile file) {
        DatingProfile profile = findActive(profileId);
        checkOwner(profile, userId);

        if (photoRepository.countByProfileId(profileId) >= 1) {
            throw new BusinessException("MVP에서는 사진 1장까지만 등록할 수 있습니다.", HttpStatus.BAD_REQUEST);
        }

        String url = objectStorageService.upload(file, "profiles/" + profileId);
        photoRepository.save(ProfilePhoto.builder()
                .profile(profile)
                .imageUrl(url)
                .displayOrder(0)
                .build());
        return url;
    }

    @Transactional
    public void deletePhoto(String profileId, String photoId, String userId) {
        DatingProfile profile = findActive(profileId);
        checkOwner(profile, userId);

        ProfilePhoto photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException("사진을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        objectStorageService.delete(photo.getImageUrl());
        photoRepository.delete(photo);
    }

    private String decryptSubjectPhone(DatingProfile profile) {
        // 신규 프로필: 등록 시 암호화 저장
        if (profile.getSubjectPhoneEncrypted() != null) {
            return phoneEncryptionUtil.decrypt(profile.getSubjectPhoneEncrypted());
        }
        // 구 프로필 fallback: B가 이미 인증 완료된 경우 subject 계정의 번호 사용
        User subject = profile.getSubject();
        if (subject != null && subject.getPhoneNumberEncrypted() != null) {
            return phoneEncryptionUtil.decrypt(subject.getPhoneNumberEncrypted());
        }
        return null;
    }

    private void validateStudentFields(boolean isStudent, String schoolName, String major, String occupation) {
        if (isStudent) {
            if (schoolName == null || schoolName.isBlank()) {
                throw new BusinessException("학생인 경우 학교명을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }
            if (major == null || major.isBlank()) {
                throw new BusinessException("학생인 경우 학과명을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }
        } else {
            if (occupation == null || occupation.isBlank()) {
                throw new BusinessException("직업을 입력해주세요.", HttpStatus.BAD_REQUEST);
            }
        }
    }

    // 매칭 성사(MATCHED) 여부 — 요청자/수신자 프로필 어느 쪽이든
    private boolean isMatched(String profileId) {
        return matchingRepository.existsByRequesterProfileIdAndStatus(profileId, MatchingStatus.MATCHED)
                || matchingRepository.existsByTargetProfileIdAndStatus(profileId, MatchingStatus.MATCHED);
    }

    private DatingProfile findActive(String profileId) {
        return profileRepository.findByIdAndStatusNot(profileId, ProfileStatus.DELETED)
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private User findUser(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
    }

    private void checkOwner(DatingProfile profile, String userId) {
        if (!profile.isOwnedBy(userId)) {
            throw new BusinessException("접근 권한이 없습니다.", HttpStatus.FORBIDDEN);
        }
    }
}
