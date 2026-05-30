package com.mydestiny.controller;

import com.mydestiny.dto.block.BlockRequest;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.BlockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/blocks")
@RequiredArgsConstructor
public class BlockController {

    private final BlockService blockService;

    @PostMapping
    public ApiResponse<Void> block(
            @AuthenticationPrincipal String userId,
            @Valid @RequestBody BlockRequest request) {
        blockService.block(userId, request.acquaintanceId());
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{acquaintanceId}")
    public ApiResponse<Void> unblock(
            @AuthenticationPrincipal String userId,
            @PathVariable String acquaintanceId) {
        blockService.unblock(userId, acquaintanceId);
        return ApiResponse.ok(null);
    }
}
