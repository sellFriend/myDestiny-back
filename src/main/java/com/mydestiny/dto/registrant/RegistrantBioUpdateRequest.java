package com.mydestiny.dto.registrant;

import jakarta.validation.constraints.Size;

public record RegistrantBioUpdateRequest(
        @Size(max = 500) String bio
) {}
