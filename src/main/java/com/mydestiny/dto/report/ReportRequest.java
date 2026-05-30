package com.mydestiny.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotBlank String reason,  // CONSENT_VIOLATION | FALSE_INFO | HARASSMENT | IMPERSONATION | OTHER
        @Size(max = 1000) String detail
) {}
