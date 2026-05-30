package com.mydestiny.service;

import com.mydestiny.domain.Acquaintance;
import com.mydestiny.domain.enums.RegistrationStatus;
import com.mydestiny.domain.enums.Visibility;
import com.mydestiny.dto.card.CardDetailResponse;
import com.mydestiny.dto.card.CardListResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.AcquaintanceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final AcquaintanceRepository acquaintanceRepository;

    @Transactional(readOnly = true)
    public List<CardListResponse> getCards(String userId) {
        return acquaintanceRepository
                .findAvailableCards(RegistrationStatus.VERIFIED, Visibility.PUBLIC, userId)
                .stream()
                .map(CardListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCardDetail(String acquaintanceId, String userId) {
        Acquaintance acquaintance = acquaintanceRepository.findById(acquaintanceId)
                .orElseThrow(() -> new BusinessException("카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (acquaintance.getRegistrationStatus() != RegistrationStatus.VERIFIED
                || acquaintance.getVisibility() != Visibility.PUBLIC
                || acquaintance.getDeletedAt() != null) {
            throw new BusinessException("조회할 수 없는 카드입니다.", HttpStatus.NOT_FOUND);
        }

        return CardDetailResponse.from(acquaintance);
    }
}
