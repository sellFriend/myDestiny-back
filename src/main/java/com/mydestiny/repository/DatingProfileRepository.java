package com.mydestiny.repository;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.enums.ProfileStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DatingProfileRepository extends JpaRepository<DatingProfile, String> {

    List<DatingProfile> findByRegistrantIdAndStatusNotOrderByCreatedAtDesc(
            String registrantId, ProfileStatus status);

    Optional<DatingProfile> findByIdAndStatusNot(String id, ProfileStatus status);

    List<DatingProfile> findByStatusOrderByPublishedAtDesc(ProfileStatus status);

    long countByRegistrantIdAndStatus(String registrantId, ProfileStatus status);
}
