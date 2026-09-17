package com.youai.crm.customer;

import jakarta.validation.constraints.NotBlank;

/** Transfers every customer owned by one employee to another employee. */
public record CustomerInheritanceRequest(
        @NotBlank String sourceOwner,
        @NotBlank String targetOwner,
        Long sourceUserId) {
}
