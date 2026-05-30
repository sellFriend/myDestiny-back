package com.mydestiny.repository;

import com.mydestiny.domain.AcquaintancePhoto;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AcquaintancePhotoRepository extends JpaRepository<AcquaintancePhoto, String> {

    List<AcquaintancePhoto> findByAcquaintanceIdOrderByDisplayOrder(String acquaintanceId);

    int countByAcquaintanceId(String acquaintanceId);
}
