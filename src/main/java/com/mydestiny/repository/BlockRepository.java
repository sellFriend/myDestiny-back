package com.mydestiny.repository;

import com.mydestiny.domain.Block;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BlockRepository extends JpaRepository<Block, String> {

    Optional<Block> findByBlockerUserIdAndBlockedAcquaintanceId(String blockerUserId, String blockedAcquaintanceId);

    boolean existsByBlockerUserIdAndBlockedAcquaintanceId(String blockerUserId, String blockedAcquaintanceId);

    void deleteByBlockerUserIdAndBlockedAcquaintanceId(String blockerUserId, String blockedAcquaintanceId);
}
