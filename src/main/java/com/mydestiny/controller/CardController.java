package com.mydestiny.controller;

import com.mydestiny.dto.card.CardDetailResponse;
import com.mydestiny.dto.card.CardListResponse;
import com.mydestiny.global.response.ApiResponse;
import com.mydestiny.service.CardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class CardController {

    private final CardService cardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CardListResponse>>> getCards(
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getCards(userId)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<CardDetailResponse>> getCardDetail(
            @PathVariable String id,
            @AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getCardDetail(id, userId)));
    }
}
