package com.mydestiny.service;

import com.mydestiny.domain.Block;
import com.mydestiny.domain.enums.NotificationType;
import com.mydestiny.global.exception.BusinessException;
import com.mydestiny.repository.AcquaintanceRepository;
import com.mydestiny.repository.BlockRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BlockService {

    private final BlockRepository blockRepository;
    private final AcquaintanceRepository acquaintanceRepository;
    private final NotificationService notificationService;

    @Transactional
    public void block(String userId, String acquaintanceId) {
        if (!acquaintanceRepository.existsById(acquaintanceId)) {
            throw new BusinessException("매물을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        if (blockRepository.existsByBlockerUserIdAndBlockedAcquaintanceId(userId, acquaintanceId)) {
            throw new BusinessException("이미 차단된 매물입니다.", HttpStatus.CONFLICT);
        }
        blockRepository.save(Block.builder()
                .blockerUserId(userId)
                .blockedAcquaintanceId(acquaintanceId)
                .build());
        notificationService.create(userId, NotificationType.ACQUAINTANCE_BLOCKED, acquaintanceId);
    }

    @Transactional
    public void unblock(String userId, String acquaintanceId) {
        if (!blockRepository.existsByBlockerUserIdAndBlockedAcquaintanceId(userId, acquaintanceId)) {
            throw new BusinessException("차단 내역을 찾을 수 없습니다.", HttpStatus.NOT_FOUND);
        }
        blockRepository.deleteByBlockerUserIdAndBlockedAcquaintanceId(userId, acquaintanceId);
    }
}
