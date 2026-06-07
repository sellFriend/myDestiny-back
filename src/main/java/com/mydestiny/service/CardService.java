package com.mydestiny.service;

import com.mydestiny.domain.DatingProfile;
import com.mydestiny.domain.enums.MatchingStatus;
import com.mydestiny.domain.enums.ProfileStatus;
import com.mydestiny.domain.enums.ProfileVisibility;
import com.mydestiny.dto.card.CardDetailResponse;
import com.mydestiny.dto.card.CardListResponse;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.DatingProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CardService {

    private final DatingProfileRepository profileRepository;

    @Transactional(readOnly = true)
    public List<CardListResponse> getCards(String userId) {
        return profileRepository
                .findAvailableCards(ProfileStatus.PUBLISHED, ProfileVisibility.PUBLIC, userId, MatchingStatus.OCCUPIED)
                .stream()
                .map(CardListResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CardDetailResponse getCardDetail(String profileId, String userId) {
        DatingProfile profile = profileRepository.findById(profileId)
                .orElseThrow(() -> new BusinessException("카드를 찾을 수 없습니다.", HttpStatus.NOT_FOUND));

        if (profile.getStatus() != ProfileStatus.PUBLISHED
                || profile.getVisibility() != ProfileVisibility.PUBLIC
                || profile.getDeletedAt() != null) {
            throw new BusinessException("조회할 수 없는 카드입니다.", HttpStatus.NOT_FOUND);
        }

        return CardDetailResponse.from(profile);
    }
}
