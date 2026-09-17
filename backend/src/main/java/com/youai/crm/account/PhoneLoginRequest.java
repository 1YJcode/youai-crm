package com.youai.crm.account;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/** Credentials used when signing in with a phone number. */
public record PhoneLoginRequest(
        @NotBlank(message = "Phone number is required")
        @Pattern(regexp = "1[3-9]\\d{9}", message = "Invalid phone number")
        String phone,

        @NotBlank(message = "Password is required")
        String password) {
}
