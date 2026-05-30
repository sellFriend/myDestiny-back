package com.mydestiny.dto.invitation;

import jakarta.validation.constraints.Size;

public record RejectRequest(
        @Size(max = 500) String reason
) {}
