package com.youai.crm.communication;

import jakarta.validation.constraints.*;

public record TelephonyEvent(
        @Pattern(regexp = "call-[0-9]+") @Size(max = 32) String requestId,
        @NotBlank @Size(max = 128) String callId,
        @NotBlank @Pattern(regexp = "INBOUND|OUTBOUND") String direction,
        @NotBlank @Pattern(regexp = "ACCEPTED|RINGING|CONNECTED|ENDED|FAILED|UNKNOWN") String state,
        @NotNull @Min(0) Long sequence,
        @Size(max = 32) String from,
        Long agentUserId,
        @Min(0) int durationSeconds,
        @Size(max = 32000) @Pattern(regexp = "(?s)v=0.*") String answerSdp) {
    TelephonyGatewayClient.Receipt receipt() {
        return new TelephonyGatewayClient.Receipt(state, callId, sequence, durationSeconds, answerSdp);
    }
}
