package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record CustomerRequest(
        @NotBlank(message = "客户姓名不能为空")
        @Size(max = 64, message = "客户姓名不能超过 64 个字符")
        String name,

        @NotBlank(message = "手机号码不能为空")
        @Pattern(regexp = "1[3-9][0-9]{9}", message = "请输入正确的 11 位手机号码")
        String phone,

        @Size(max = 128, message = "公司名称不能超过 128 个字符")
        String company,

        @NotBlank(message = "客户来源不能为空")
        String source,

        @NotBlank(message = "负责人不能为空")
        String owner,

        String stage,
        String level,

        @PositiveOrZero(message = "预计金额不能小于 0")
        BigDecimal amount,

        String city,
        @Size(max = 1000, message = "备注不能超过 1000 个字符")
        String note,
        LocalDateTime nextFollowAt,
        String gender,
        String birthday,
        String age,
        String height,
        String maritalStatus,
        String education,
        String monthlyIncome,
        String annualIncome,
        String occupation,
        String housing,
        String car,
        String nativePlace,
        String workLocation,
        String wechat,
        String idCard,
        String remark,
        String certificationStatus, String familyStatus, String childrenStatus, String vehicleHousing,
        Integer registrationCount, String matchAgeRange, String matchMaritalStatus, String matchHeightRange,
        String matchEducation, String matchMonthlyIncome, String matchMostImportant, String matchPersonality,
        String matchChildren, String matchDealbreakers, String collaborator,
        String customerType, LocalDateTime lastLoginAt,
        @Size(max = 200000, message = "头像图片过大，请重新选择") String avatarUrl,
        List<String> tags) {
}
