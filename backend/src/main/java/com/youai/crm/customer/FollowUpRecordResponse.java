package com.youai.crm.customer;

import java.time.LocalDateTime;

public record FollowUpRecordResponse(
        String id,
        String customerId,
        String customer,
        String type,
        String channel,
        String title,
        String content,
        String customerStatus,
        String owner,
        LocalDateTime occurredAt,
        boolean completed,
        String category,com.youai.crm.communication.CallFollowupWorkService.Source source) {
    public FollowUpRecordResponse(String id,String customerId,String customer,String type,String channel,String title,String content,String customerStatus,String owner,LocalDateTime occurredAt,boolean completed,String category){
        this(id,customerId,customer,type,channel,title,content,customerStatus,owner,occurredAt,completed,category,null);
    }
    public FollowUpRecordResponse withSource(com.youai.crm.communication.CallFollowupWorkService.Source source){return new FollowUpRecordResponse(id,customerId,customer,type,channel,title,content,customerStatus,owner,occurredAt,completed,category,source);}
}
