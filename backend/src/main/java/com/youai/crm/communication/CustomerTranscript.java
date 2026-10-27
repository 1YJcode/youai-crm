package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_customer_transcript")
public class CustomerTranscript {
    @Id @Column(length=32) String customerNo;
    @Column(nullable=false) LocalDateTime createdAt;
    protected CustomerTranscript() {}
    CustomerTranscript(String customerNo) {this.customerNo=customerNo;createdAt=LocalDateTime.now();}
}
