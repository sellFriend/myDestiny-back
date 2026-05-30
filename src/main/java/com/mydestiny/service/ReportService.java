package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Report;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.dto.report.ReportRequest;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.ReportRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
public class ReportService {

    private static final Set<String> VALID_REASONS =
            Set.of("CONSENT_VIOLATION", "FALSE_INFO", "HARASSMENT", "IMPERSONATION", "OTHER");

    private final DatingProfileRepository profileRepository;
    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    @Transactional
    public void report(String profileId, String reporterId, ReportRequest req) {
        if (!VALID_REASONS.contains(req.reason())) {
            throw new BusinessException("유효하지 않은 신고 사유입니다.", HttpStatus.BAD_REQUEST);
        }

        DatingProfile profile = profileRepository.findByIdAndStatusNot(profileId, ProfileStatus.DELETED)
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (profile.getStatus() == ProfileStatus.DELETED
                || profile.getStatus() == ProfileStatus.DRAFT) {
            throw new BusinessException("신고할 수 없는 상태입니다.", HttpStatus.BAD_REQUEST);
        }

        // 중복 신고 방지
        if (reportRepository.existsByProfileIdAndReporterId(profileId, reporterId)) {
            throw new BusinessException("이미 신고한 프로필입니다.", HttpStatus.CONFLICT);
        }

        User reporter = userRepository.getReferenceById(reporterId);

        reportRepository.save(Report.builder()
                .profile(profile)
                .reporter(reporter)
                .reason(req.reason())
                .detail(req.detail())
                .build());

        // 신고 접수 시 REPORTED 상태로 전환 (공개 유지 + 플래그)
        if (profile.getStatus() == ProfileStatus.PUBLISHED) {
            profile.markReported();
            profileRepository.save(profile);
        }
    }
}
