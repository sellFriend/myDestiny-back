package com.mydestiny.service;

import com.mydestiny.domain.Follow;
import com.mydestiny.domain.User;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.FollowRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;

    public void follow(String followerId, String followingId) {
        if (followerId.equals(followingId)) {
            throw new BusinessException("자기 자신을 팔로우할 수 없습니다.", HttpStatus.BAD_REQUEST);
        }
        if (followRepository.existsByFollowerIdAndFollowingId(followerId, followingId)) {
            throw new BusinessException("이미 팔로우 중입니다.", HttpStatus.CONFLICT);
        }
        User follower = userRepository.getReferenceById(followerId);
        User following = userRepository.getReferenceById(followingId);
        followRepository.save(Follow.builder().follower(follower).following(following).build());
    }

    public void unfollow(String followerId, String followingId) {
        Follow follow = followRepository.findByFollowerIdAndFollowingId(followerId, followingId)
                .orElseThrow(() -> new BusinessException("팔로우 관계가 없습니다.", HttpStatus.NOT_FOUND));
        followRepository.delete(follow);
    }

    @Transactional(readOnly = true)
    public FollowStatus getFollowStatus(String currentUserId, String targetUserId) {
        boolean following = followRepository.existsByFollowerIdAndFollowingId(currentUserId, targetUserId);
        boolean followedBack = followRepository.existsByFollowerIdAndFollowingId(targetUserId, currentUserId);
        return new FollowStatus(following, followedBack && following,
                followRepository.countByFollowingId(targetUserId),
                followRepository.countByFollowerId(targetUserId));
    }

    @Transactional(readOnly = true)
    public Set<String> getMutualFollowIds(String userId) {
        return followRepository.findMutualFollowIds(userId);
    }

    public record FollowStatus(boolean isFollowing, boolean isMutual,
                               long followerCount, long followingCount) {}
}
