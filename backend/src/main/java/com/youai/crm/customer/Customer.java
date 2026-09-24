package com.youai.crm.customer;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "crm_customer")
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_no", nullable = false, unique = true, length = 32)
    private String customerNo;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(nullable = false, unique = true, length = 24)
    private String phone;

    @Column(nullable = false, length = 128)
    private String company;

    @Column(nullable = false, length = 32)
    private String source;

    @Column(nullable = false, length = 32)
    private String owner;

    @Column(nullable = false, length = 32)
    private String stage;

    @Column(nullable = false, length = 32)
    private String level;

    @Column(name = "expected_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal expectedAmount = BigDecimal.ZERO;

    @Column(length = 64)
    private String city;

    @Column(length = 1000)
    private String note;

    private String gender;
    private String birthday;
    private String age;
    private String height;
    private String maritalStatus;
    private String education;
    private String monthlyIncome;
    private String annualIncome;
    private String occupation;
    private String housing;
    private String car;
    private String nativePlace;
    private String workLocation;
    private String wechat;
    private String idCard;
    private String remark;
    private String certificationStatus;
    private String familyStatus;
    private String childrenStatus;
    private String vehicleHousing;
    @Column(nullable = false)
    private Integer registrationCount = 0;
    private String matchAgeRange;
    private String matchMaritalStatus;
    private String matchHeightRange;
    private String matchEducation;
    private String matchMonthlyIncome;
    private String matchMostImportant;
    private String matchPersonality;
    private String matchChildren;
    private String matchDealbreakers;

    @Column(length = 255)
    private String collaborator;

    @Column(name = "last_contact_at")
    private LocalDateTime lastContactAt;

    @Column(name = "next_follow_at")
    private LocalDateTime nextFollowAt;

    @Column(name = "first_allocation_at")
    private LocalDateTime firstAllocationAt;

    @Column(name = "last_allocation_at")
    private LocalDateTime lastAllocationAt;

    @Column(name = "previous_owner", length = 32)
    private String previousOwner;

    @Column(name = "pool_entry_type", length = 64)
    private String poolEntryType;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "crm_customer_tag", joinColumns = @JoinColumn(name = "customer_id"))
    @OrderColumn(name = "sort_order")
    @Column(name = "tag", nullable = false, length = 32)
    private List<String> tags = new ArrayList<>();

    @Version
    private Long version;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getCustomerNo() { return customerNo; }
    public void setCustomerNo(String customerNo) { this.customerNo = customerNo; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }
    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    public BigDecimal getExpectedAmount() { return expectedAmount; }
    public void setExpectedAmount(BigDecimal expectedAmount) { this.expectedAmount = expectedAmount; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    public String getAge() { return age; }
    public void setAge(String age) { this.age = age; }
    public String getHeight() { return height; }
    public void setHeight(String height) { this.height = height; }
    public String getMaritalStatus() { return maritalStatus; }
    public void setMaritalStatus(String maritalStatus) { this.maritalStatus = maritalStatus; }
    public String getEducation() { return education; }
    public void setEducation(String education) { this.education = education; }
    public String getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(String monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public String getAnnualIncome() { return annualIncome; }
    public void setAnnualIncome(String annualIncome) { this.annualIncome = annualIncome; }
    public String getOccupation() { return occupation; }
    public void setOccupation(String occupation) { this.occupation = occupation; }
    public String getHousing() { return housing; }
    public void setHousing(String housing) { this.housing = housing; }
    public String getCar() { return car; }
    public void setCar(String car) { this.car = car; }
    public String getNativePlace() { return nativePlace; }
    public void setNativePlace(String nativePlace) { this.nativePlace = nativePlace; }
    public String getWorkLocation() { return workLocation; }
    public void setWorkLocation(String workLocation) { this.workLocation = workLocation; }
    public String getWechat() { return wechat; }
    public void setWechat(String wechat) { this.wechat = wechat; }
    public String getIdCard() { return idCard; }
    public void setIdCard(String idCard) { this.idCard = idCard; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getCertificationStatus() { return certificationStatus; }
    public void setCertificationStatus(String value) { certificationStatus = value; }
    public String getFamilyStatus() { return familyStatus; }
    public void setFamilyStatus(String value) { familyStatus = value; }
    public String getChildrenStatus() { return childrenStatus; }
    public void setChildrenStatus(String value) { childrenStatus = value; }
    public String getVehicleHousing() { return vehicleHousing; }
    public void setVehicleHousing(String value) { vehicleHousing = value; }
    public Integer getRegistrationCount() { return registrationCount; }
    public void setRegistrationCount(Integer value) { registrationCount = value == null ? 0 : value; }
    public String getMatchAgeRange() { return matchAgeRange; }
    public void setMatchAgeRange(String value) { matchAgeRange = value; }
    public String getMatchMaritalStatus() { return matchMaritalStatus; }
    public void setMatchMaritalStatus(String value) { matchMaritalStatus = value; }
    public String getMatchHeightRange() { return matchHeightRange; }
    public void setMatchHeightRange(String value) { matchHeightRange = value; }
    public String getMatchEducation() { return matchEducation; }
    public void setMatchEducation(String value) { matchEducation = value; }
    public String getMatchMonthlyIncome() { return matchMonthlyIncome; }
    public void setMatchMonthlyIncome(String value) { matchMonthlyIncome = value; }
    public String getMatchMostImportant() { return matchMostImportant; }
    public void setMatchMostImportant(String value) { matchMostImportant = value; }
    public String getMatchPersonality() { return matchPersonality; }
    public void setMatchPersonality(String value) { matchPersonality = value; }
    public String getMatchChildren() { return matchChildren; }
    public void setMatchChildren(String value) { matchChildren = value; }
    public String getMatchDealbreakers() { return matchDealbreakers; }
    public void setMatchDealbreakers(String value) { matchDealbreakers = value; }
    public String getCollaborator() { return collaborator; }
    public void setCollaborator(String value) { collaborator = value; }
    public LocalDateTime getLastContactAt() { return lastContactAt; }
    public void setLastContactAt(LocalDateTime lastContactAt) { this.lastContactAt = lastContactAt; }
    public LocalDateTime getNextFollowAt() { return nextFollowAt; }
    public void setNextFollowAt(LocalDateTime nextFollowAt) { this.nextFollowAt = nextFollowAt; }
    public LocalDateTime getFirstAllocationAt() { return firstAllocationAt; }
    public void setFirstAllocationAt(LocalDateTime firstAllocationAt) { this.firstAllocationAt = firstAllocationAt; }
    public LocalDateTime getLastAllocationAt() { return lastAllocationAt; }
    public void setLastAllocationAt(LocalDateTime lastAllocationAt) { this.lastAllocationAt = lastAllocationAt; }
    public String getPreviousOwner() { return previousOwner; }
    public void setPreviousOwner(String previousOwner) { this.previousOwner = previousOwner; }
    public String getPoolEntryType() { return poolEntryType; }
    public void setPoolEntryType(String poolEntryType) { this.poolEntryType = poolEntryType; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = new ArrayList<>(tags); }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
