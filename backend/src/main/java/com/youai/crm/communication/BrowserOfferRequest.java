package com.youai.crm.communication;

public record BrowserOfferRequest(
        @jakarta.validation.constraints.NotBlank
        @jakarta.validation.constraints.Size(max = 32000)
        @jakarta.validation.constraints.Pattern(regexp = "(?s)v=0.*") String offerSdp) {
}
