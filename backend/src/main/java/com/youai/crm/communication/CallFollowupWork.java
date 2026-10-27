package com.youai.crm.communication;
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity @Table(name="crm_call_followup_work")
public class CallFollowupWork {
    @Id @Column(name="call_id") public Long callId;
    @Column(name="customer_no",nullable=false) public String customerNo;
    @Column(name="started_at",nullable=false) public LocalDateTime startedAt;
    @Column(name="remind_at",nullable=false) public LocalDateTime remindAt;
    @Column(name="escalate_at",nullable=false) public LocalDateTime escalateAt;
    @Column(name="follow_up_id") public Long followUpId;
    @Column(name="completed_at") public LocalDateTime completedAt;
    @Column(name="source_segment_id") public Long sourceSegmentId;
    @Column(name="analysis_version") public Long analysisVersion;
    @Column(length=64) public String template;
    @Column(name="confirmed_content",columnDefinition="TEXT") public String confirmedContent;
    @Column(name="confirmed_by") public Long confirmedBy;
    @Column(name="confirmed_by_name",length=64) public String confirmedByName;
}
