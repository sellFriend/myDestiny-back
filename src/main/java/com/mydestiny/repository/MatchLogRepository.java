package com.mydestiny.repository;

import com.mydestiny.domain.MatchLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MatchLogRepository extends JpaRepository<MatchLog, String> {

    List<MatchLog> findByMatchingIdOrderByCreatedAtAsc(String matchingId);
}
