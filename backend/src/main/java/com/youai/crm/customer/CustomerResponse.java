package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record CustomerResponse(
        String id,
        String name,
        String nickname,
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
        Integer deepTalkDurationSeconds, Long ownerId, java.util.Set<Long> collaboratorIds) {

    public static CustomerResponse from(Customer customer) {
        // Keep the safe projection as the default so a new caller cannot
        // accidentally expose private customer data.
        return from(customer, false);
    }

    /**
     * The visibility flag is the authorization result calculated by the
     * service: only an administrator or the current owner may receive the
     * contact fields. Unauthorized responses contain no partial identifiers.
     */
    public static CustomerResponse from(Customer customer, boolean contactVisible) {
        return from(customer, contactVisible, 0);
    }

    public static CustomerResponse from(Customer customer, boolean contactVisible, int deepTalkDurationSeconds) {
        return new CustomerResponse(
                customer.getCustomerNo(),
                customer.getName(),
                customer.getNickname(),
                contactVisible ? customer.getPhone() : null,
                contactVisible ? customer.getCompany() : null,
                contactVisible ? customer.getSource() : null,
                customer.getOwner(),
                customer.getStage(),
                customer.getLevel(),
                customer.getExpectedAmount(),
                customer.getCity(),
                contactVisible ? customer.getNote() : null,
                customer.getGender(),
                contactVisible ? customer.getBirthday() : null,
                customer.getAge(),
                customer.getHeight(),
                customer.getMaritalStatus(),
                customer.getEducation(),
                customer.getMonthlyIncome(),
                customer.getAnnualIncome(),
                customer.getOccupation(),
                customer.getHousing(),
                customer.getCar(),
                contactVisible ? customer.getNativePlace() : null,
                contactVisible ? customer.getWorkLocation() : null,
                contactVisible ? customer.getWechat() : null,
                contactVisible,
                contactVisible ? customer.getIdCard() : null,
                contactVisible ? customer.getRemark() : null,
                customer.getCertificationStatus(), contactVisible ? customer.getFamilyStatus() : null, contactVisible ? customer.getChildrenStatus() : null, contactVisible ? customer.getVehicleHousing() : null,
                customer.getRegistrationCount(), customer.getMatchAgeRange(), customer.getMatchMaritalStatus(), customer.getMatchHeightRange(),
                customer.getMatchEducation(), customer.getMatchMonthlyIncome(), contactVisible ? customer.getMatchMostImportant() : null, contactVisible ? customer.getMatchPersonality() : null,
                contactVisible ? customer.getMatchChildren() : null, contactVisible ? customer.getMatchDealbreakers() : null, customer.getCollaborator(),
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
                contactVisible ? List.copyOf(customer.getTags()) : List.of(),
                Math.max(0, deepTalkDurationSeconds), customer.getOwnerId(), java.util.Set.copyOf(customer.getCollaboratorIds()));
    }

}
