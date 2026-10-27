package com.youai.crm.communication;

import java.time.LocalDateTime;

public record CallResponse(
        Long id,
        String customerId,
        String customer,
        String phone,
        String owner,
        String agent,
        String direction,
        String status,
        int durationSeconds,
        String note,
        LocalDateTime startedAt, String recordingSource, Long agentUserId, Long uploadedByUserId, LocalDateTime uploadedAt, String customerOwnershipStatus, boolean canAccessRecording) {

    public static CallResponse from(CallRecord call, String ownershipStatus, boolean canAccessRecording) {
        return new CallResponse(call.getId(), call.getCustomerNo(), call.getCustomerName(), canAccessRecording ? call.getPhone() : "",
                canAccessRecording ? call.getOwner() : "", call.getAgent(), call.getDirection(), call.getStatus(), call.getDurationSeconds(),
                canAccessRecording ? call.getNote() : "", call.getStartedAt(), call.getRecordingSource(), call.getAgentUserId(), call.getUploadedByUserId(), call.getUploadedAt(), ownershipStatus, canAccessRecording);
    }
}
