package com.mydestiny.repository;

import com.mydestiny.domain.PendingInvite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PendingInviteRepository extends JpaRepository<PendingInvite, String> {

    Optional<PendingInvite> findByToken(String token);

    void deleteByToken(String token);
}
