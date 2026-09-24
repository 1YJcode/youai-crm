package com.youai.crm.system;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record UpdateCustomerPoolRuleRequest(
        @NotNull Boolean enabled,
        @NotNull @Min(1) @Max(3650) Integer days) { }
