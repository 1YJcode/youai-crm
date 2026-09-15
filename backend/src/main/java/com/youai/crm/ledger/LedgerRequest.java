package com.youai.crm.ledger;
import java.math.BigDecimal;
import jakarta.validation.constraints.*;
public record LedgerRequest(@NotBlank String orderNo, @NotBlank String storeName, @NotBlank String beneficiary, @NotNull @DecimalMin("0.01") BigDecimal ledgerAmount) {}
