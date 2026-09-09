package com.youke.crm.invitation;

import java.time.LocalDateTime;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record InvitationRequest(
    @NotBlank String customerId,
    @NotBlank String invitationMethod,
    @NotBlank String storeName,
    @NotNull LocalDateTime scheduledAt,
    String remark) {}
