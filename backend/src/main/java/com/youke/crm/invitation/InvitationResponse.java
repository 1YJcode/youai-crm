package com.youke.crm.invitation;

import java.time.LocalDateTime;

public record InvitationResponse(
    String id, String customerId, String customerName, String gender, String birthYear,
    String inviter, String department, String invitationMethod, String storeName,
    LocalDateTime createdAt, LocalDateTime scheduledAt, LocalDateTime arrivalAt,
    String arrivalStatus, String maritalStatus, String annualIncome, String source,
    String referrer, String relatedOrderNo, String remark) {}
