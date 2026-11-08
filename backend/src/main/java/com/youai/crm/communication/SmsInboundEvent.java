package com.youai.crm.communication;

public record SmsInboundEvent(
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 128) String messageId,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 32) String from,
        @jakarta.validation.constraints.NotBlank @jakarta.validation.constraints.Size(max = 2000) String content) {
}
