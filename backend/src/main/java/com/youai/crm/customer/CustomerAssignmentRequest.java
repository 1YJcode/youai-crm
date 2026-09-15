package com.youai.crm.customer;

import jakarta.validation.constraints.NotBlank;

public record CustomerAssignmentRequest(
        @NotBlank(message = "负责人不能为空") String owner,
        String type,
        String maturity,
        String reason) {
}
