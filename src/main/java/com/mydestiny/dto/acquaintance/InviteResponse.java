package com.mydestiny.dto.acquaintance;

import java.time.LocalDateTime;

public record InviteResponse(String formUrl, LocalDateTime expiresAt) {}
