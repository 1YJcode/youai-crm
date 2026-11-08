package com.youai.crm.communication;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record TelephonyRequest(
        @NotBlank @Size(max = 32) String customerId,
        @NotBlank @Pattern(regexp = "[a-zA-Z0-9-]{16,64}") String requestId,
        @NotBlank @Size(max = 32000) @Pattern(regexp = "(?s)v=0.*") String offerSdp) {
}
