package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.Report;
import com.mydestiny.domain.User;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.Role;
import com.mydestiny.dto.admin.AdminProfileResponse;
import com.mydestiny.dto.admin.AdminReportResponse;
import com.mydestiny.dto.admin.AdminReportUpdateRequest;
import com.mydestiny.dto.admin.AdminReviewRequest;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.dto.admin.AdminMatchingResponse;
import com.mydestiny.repository.DatingProfileRepository;
import com.mydestiny.repository.MatchingRepository;
import com.mydestiny.repository.ReportRepository;
import com.mydestiny.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final DatingProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final ReportRepository reportRepository;
    private final MatchingRepository matchingRepository;

    @Transactional(readOnly = true)
    public List<AdminProfileResponse> getProfiles(String adminId, ProfileStatus status) {
        checkAdmin(adminId);
        return profileRepository
                .findByRegistrantIdAndStatusNotOrderByCreatedAtDesc(adminId, ProfileStatus.DELETED)
                .stream()
                // 관리자는 전체 조회이므로 별도 쿼리 사용
                .filter(p -> status == null || p.getStatus() == status)
                .map(AdminProfileResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminProfileResponse> getAllByStatus(String adminId, ProfileStatus status) {
        checkAdmin(adminId);
        return profileRepository.findAll().stream()
                .filter(p -> status == null || p.getStatus() == status)
                .filter(p -> p.getStatus() != ProfileStatus.DELETED)
                .map(AdminProfileResponse::from)
                .toList();
    }

    @Transactional
    public ProfileDetailResponse review(String adminId, String profileId, AdminReviewRequest req) {
        checkAdmin(adminId);

        DatingProfile profile = profileRepository.findByIdAndStatusNot(profileId, ProfileStatus.DELETED)
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        switch (req.decision().toUpperCase()) {
            case "APPROVE" -> profile.adminApprove();
            case "REJECT"  -> profile.adminReject(req.note());
            default -> throw new BusinessException("decision은 APPROVE 또는 REJECT여야 합니다.", HttpStatus.BAD_REQUEST);
        }

        profileRepository.save(profile);
        return ProfileDetailResponse.from(profile, null);
    }

    @Transactional
    public void suspend(String adminId, String profileId) {
        checkAdmin(adminId);
        DatingProfile profile = profileRepository.findByIdAndStatusNot(profileId, ProfileStatus.DELETED)
                .orElseThrow(() -> new BusinessException("프로필을 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        profile.suspend();
        profileRepository.save(profile);
    }

    @Transactional(readOnly = true)
    public List<AdminMatchingResponse> getMatchings(String adminId, String status) {
        checkAdmin(adminId);
        return matchingRepository.findAllByOrderByCreatedAtDesc().stream()
                .filter(m -> status == null || m.getStatus().name().equals(status))
                .map(AdminMatchingResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AdminReportResponse> getReports(String adminId, String status) {
        checkAdmin(adminId);
        return (status != null
                ? reportRepository.findByStatusOrderByCreatedAtDesc(status)
                : reportRepository.findAll())
                .stream()
                .map(AdminReportResponse::from)
                .toList();
    }

    @Transactional
    public AdminReportResponse updateReport(String adminId, String reportId, AdminReportUpdateRequest req) {
        checkAdmin(adminId);
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException("신고를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        report.updateStatus(req.status());
        return AdminReportResponse.from(reportRepository.save(report));
    }

    private void checkAdmin(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));
        if (user.getRole() != Role.ADMIN) {
            throw new BusinessException("관리자 권한이 필요합니다.", HttpStatus.FORBIDDEN);
        }
    }
}
