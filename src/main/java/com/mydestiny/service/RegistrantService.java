package com.mydestiny.service;

import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.registrant.RegistrantSummaryResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.FollowRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RegistrantService {

    private final UserRepository userRepository;
    private final DatingProfileRepository profileRepository;
    private final FollowRepository followRepository;

    @Transactional(readOnly = true)
    public List<RegistrantSummaryResponse> getRegistrants(String currentUserId) {
        return userRepository.findActiveRegistrants(ProfileStatus.PUBLISHED, currentUserId)
                .stream()
                .map(u -> RegistrantSummaryResponse.of(
                        u,
                        profileRepository.countByRegistrantIdAndStatus(u.getId(), ProfileStatus.PUBLISHED),
                        followRepository.countByFollowingId(u.getId()),
                        followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public RegistrantSummaryResponse getRegistrant(String targetUserId, String currentUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        return RegistrantSummaryResponse.of(
                user,
                profileRepository.countByRegistrantIdAndStatus(targetUserId, ProfileStatus.PUBLISHED),
                followRepository.countByFollowingId(targetUserId),
                followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId)
        );
    }

    @Transactional(readOnly = true)
    public List<RegistrantSummaryResponse> getFollowing(String targetUserId, String currentUserId) {
        return followRepository.findFollowingUsers(targetUserId)
                .stream()
                .map(u -> RegistrantSummaryResponse.of(
                        u,
                        profileRepository.countByRegistrantIdAndStatus(u.getId(), ProfileStatus.PUBLISHED),
                        followRepository.countByFollowingId(u.getId()),
                        followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId())
                ))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistrantSummaryResponse> getFollowers(String targetUserId, String currentUserId) {
        return followRepository.findFollowerUsers(targetUserId)
                .stream()
                .map(u -> RegistrantSummaryResponse.of(
                        u,
                        profileRepository.countByRegistrantIdAndStatus(u.getId(), ProfileStatus.PUBLISHED),
                        followRepository.countByFollowingId(u.getId()),
                        followRepository.existsByFollowerIdAndFollowingId(currentUserId, u.getId())
                ))
                .toList();
    }

    @Transactional
    public void updateBio(String userId, String bio) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        user.updateBio(bio);
        userRepository.save(user);
    }
}
