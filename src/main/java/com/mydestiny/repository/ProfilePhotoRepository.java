package com.mydestiny.repository;

import com.mydestiny.domain.ProfilePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProfilePhotoRepository extends JpaRepository<ProfilePhoto, String> {

    int countByProfileId(String profileId);

    List<ProfilePhoto> findByProfileIdOrderByDisplayOrder(String profileId);
}
