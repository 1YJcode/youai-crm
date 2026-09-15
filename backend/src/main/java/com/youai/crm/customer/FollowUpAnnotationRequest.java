package com.youai.crm.customer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FollowUpAnnotationRequest(
        @NotBlank String recordId,
        @NotBlank String recordType,
        boolean favorite,
        @Size(max = 2000) String comment) {
}
