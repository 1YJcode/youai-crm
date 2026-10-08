package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        String id,
        String name,
        String phone,
        String company,
        String source,
        String owner,
        String stage,
        String level,
        BigDecimal amount,
        String city,
        String note,
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
        boolean contactVisible,
        String idCard,
        String remark,
        String certificationStatus, String familyStatus, String childrenStatus, String vehicleHousing,
        Integer registrationCount, String matchAgeRange, String matchMaritalStatus, String matchHeightRange,
        String matchEducation, String matchMonthlyIncome, String matchMostImportant, String matchPersonality,
        String matchChildren, String matchDealbreakers, String collaborator,
        String customerType,
        LocalDateTime lastLoginAt,
        String avatarUrl,
        LocalDateTime lastContactAt,
        LocalDateTime nextFollowAt,
        LocalDateTime firstAllocationAt,
        LocalDateTime lastAllocationAt,
        String previousOwner,
        String poolEntryType,
        LocalDateTime poolEnteredAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> tags,
        Integer deepTalkDurationSeconds) {

    public static CustomerResponse from(Customer customer) {
        // Keep the safe projection as the default so a new caller cannot
        // accidentally expose private customer data.
        return from(customer, false);
    }

    /**
     * The visibility flag is the authorization result calculated by the
     * service: only an administrator or the current owner may receive the
     * unmasked contact fields.
     */
    public static CustomerResponse from(Customer customer, boolean contactVisible) {
        return from(customer, contactVisible, 0);
    }

    public static CustomerResponse from(Customer customer, boolean contactVisible, int deepTalkDurationSeconds) {
        return new CustomerResponse(
                customer.getCustomerNo(),
                customer.getName(),
                contactVisible ? customer.getPhone() : maskPhone(customer.getPhone()),
                customer.getCompany(),
                customer.getSource(),
                customer.getOwner(),
                customer.getStage(),
                customer.getLevel(),
                customer.getExpectedAmount(),
                customer.getCity(),
                customer.getNote(),
                customer.getGender(),
                customer.getBirthday(),
                customer.getAge(),
                customer.getHeight(),
                customer.getMaritalStatus(),
                customer.getEducation(),
                customer.getMonthlyIncome(),
                customer.getAnnualIncome(),
                customer.getOccupation(),
                customer.getHousing(),
                customer.getCar(),
                customer.getNativePlace(),
                customer.getWorkLocation(),
                contactVisible ? customer.getWechat() : maskWechat(customer.getWechat()),
                contactVisible,
                customer.getIdCard(),
                customer.getRemark(),
                customer.getCertificationStatus(), customer.getFamilyStatus(), customer.getChildrenStatus(), customer.getVehicleHousing(),
                customer.getRegistrationCount(), customer.getMatchAgeRange(), customer.getMatchMaritalStatus(), customer.getMatchHeightRange(),
                customer.getMatchEducation(), customer.getMatchMonthlyIncome(), customer.getMatchMostImportant(), customer.getMatchPersonality(),
                customer.getMatchChildren(), customer.getMatchDealbreakers(), customer.getCollaborator(),
                customer.getCustomerType(), customer.getLastLoginAt(), customer.getAvatarUrl(),
                customer.getLastContactAt(),
                customer.getNextFollowAt(),
                customer.getFirstAllocationAt(),
                customer.getLastAllocationAt(),
                customer.getPreviousOwner(),
                customer.getPoolEntryType(),
                customer.getPoolEnteredAt(),
                customer.getCreatedAt(),
                customer.getUpdatedAt(),
                List.copyOf(customer.getTags()),
                Math.max(0, deepTalkDurationSeconds));
    }

    private static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) return phone;
        String compact = phone.replaceAll("\\s+", "");
        if (compact.matches("\\d{11}")) return compact.substring(0, 3) + "****" + compact.substring(7);
        if (compact.length() <= 5) return "******";
        return compact.substring(0, 3) + "****" + compact.substring(compact.length() - 2);
    }

    private static String maskWechat(String wechat) {
        if (wechat == null || wechat.isBlank()) return wechat;
        String value = wechat.trim();
        if (value.length() <= 2) return "******";
        return value.substring(0, 1) + "****" + value.substring(value.length() - 1);
    }
}
