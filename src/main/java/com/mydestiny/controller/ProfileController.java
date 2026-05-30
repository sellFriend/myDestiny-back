package com.mydestiny.controller;

import com.mydestiny.domain.enums.ProfileVisibility;
import com.mydestiny.dto.profile.ProfileCreateRequest;
import com.mydestiny.dto.profile.ProfileDetailResponse;
import com.mydestiny.dto.profile.ProfileSummaryResponse;
import com.mydestiny.dto.profile.PublicProfileResponse;
import com.mydestiny.dto.profile.ProfileUpdateRequest;
import com.mydestiny.dto.report.ReportRequest;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.ProfileService;
import com.mydestiny.service.ReportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/profiles")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final ReportService reportService;

    @PostMapping
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> create(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ProfileCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(profileService.create(userId, request)));
    }

    @GetMapping("/public")
    public ResponseEntity<ApiResponse<List<PublicProfileResponse>>> getPublicProfiles(
            @AuthenticationPrincipal String userId,
            @RequestParam(required = false) String registrantId,
            @RequestParam(required = false) String gender) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getPublicProfiles(userId, registrantId, gender)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<ProfileSummaryResponse>>> getMyProfiles(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getMyProfiles(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> getDetail(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.getDetail(id, userId)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<ProfileDetailResponse>> update(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ProfileUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.update(id, userId, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        profileService.delete(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/photos")
    public ResponseEntity<ApiResponse<String>> uploadPhoto(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.ok(profileService.uploadPhoto(id, userId, file)));
    }

    @DeleteMapping("/{id}/photos/{photoId}")
    public ResponseEntity<ApiResponse<Void>> deletePhoto(
            @PathVariable String id,
            @PathVariable String photoId,
            @AuthenticationPrincipal String userId) {
        profileService.deletePhoto(id, photoId, userId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PatchMapping("/{id}/visibility")
    public ResponseEntity<ApiResponse<Void>> changeVisibility(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @RequestParam String visibility) {
        profileService.changeVisibility(id, userId, ProfileVisibility.valueOf(visibility));
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/{id}/reports")
    public ResponseEntity<ApiResponse<Void>> report(
            @PathVariable String id,
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody ReportRequest request) {
        reportService.report(id, userId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
