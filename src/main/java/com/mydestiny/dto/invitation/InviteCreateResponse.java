package com.mydestiny.dto.invitation;

import java.time.LocalDateTime;

public record InviteCreateResponse(
        String inviteUrl,
        LocalDateTime expiresAt
) {}
