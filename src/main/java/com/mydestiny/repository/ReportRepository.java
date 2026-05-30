package com.mydestiny.repository;

import com.mydestiny.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReportRepository extends JpaRepository<Report, String> {

    List<Report> findByStatusOrderByCreatedAtDesc(String status);

    boolean existsByProfileIdAndReporterId(String profileId, String reporterId);
}
