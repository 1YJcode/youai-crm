package com.youai.crm.order;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name="crm_sales_contract")
public class SalesContract {
 @Version @Column(nullable=false) public long version;
 @Id @GeneratedValue(strategy=GenerationType.IDENTITY) public Long id;
 @Column(name="contract_no", nullable=false, unique=true, length=64) public String contractNo;
 @OneToOne(optional=false) @JoinColumn(name="order_no", referencedColumnName="order_no", unique=true) public SalesOrder order;
 @Column(nullable=false, length=24) public String status="待审核";
 @Column(name="created_at",nullable=false) public LocalDateTime createdAt;
 @Column(name="reviewed_at") public LocalDateTime reviewedAt;
 @Column(name="reviewed_by") public Long reviewedBy;
 @Column(name="review_note",length=500) public String reviewNote;
 public static SalesContract from(SalesOrder order) {
  SalesContract c=new SalesContract(); c.order=order;
  c.contractNo="HT-"+order.getOrderNo(); c.createdAt=order.getCreatedAt(); return c;
 }
}
