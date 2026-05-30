package com.mydestiny.repository;

import com.mydestiny.domain.Follow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface FollowRepository extends JpaRepository<Follow, String> {

    Optional<Follow> findByFollowerIdAndFollowingId(String followerId, String followingId);

    boolean existsByFollowerIdAndFollowingId(String followerId, String followingId);

    // 내가 팔로우하는 사람 ID 목록
    @Query("SELECT f.following.id FROM Follow f WHERE f.follower.id = :userId")
    Set<String> findFollowingIdsByFollowerId(@Param("userId") String userId);

    // 나를 팔로우하는 사람 ID 목록
    @Query("SELECT f.follower.id FROM Follow f WHERE f.following.id = :userId")
    Set<String> findFollowerIdsByFollowingId(@Param("userId") String userId);

    // 맞팔로우 중인 사용자 ID 목록 (내가 팔로우 AND 나를 팔로우)
    @Query("""
            SELECT f1.following.id FROM Follow f1
            WHERE f1.follower.id = :userId
              AND EXISTS (
                  SELECT 1 FROM Follow f2
                  WHERE f2.follower.id = f1.following.id
                    AND f2.following.id = :userId
              )
            """)
    Set<String> findMutualFollowIds(@Param("userId") String userId);

    long countByFollowingId(String followingId);
    long countByFollowerId(String followerId);

    @Query("SELECT f.following FROM Follow f WHERE f.follower.id = :userId")
    List<com.mydestiny.domain.User> findFollowingUsers(@Param("userId") String userId);

    @Query("SELECT f.follower FROM Follow f WHERE f.following.id = :userId")
    List<com.mydestiny.domain.User> findFollowerUsers(@Param("userId") String userId);
}
