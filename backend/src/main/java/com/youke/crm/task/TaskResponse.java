package com.youke.crm.task;

import java.time.LocalDateTime;

public record TaskResponse(
        Long id,
        String title,
        String customer,
        String customerId,
        String customerStatus,
        String owner,
        LocalDateTime dueAt,
        String type,
        String status,
        boolean done,
        String priority,
        LocalDateTime followedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {

    public static TaskResponse from(FollowUpTask task) {
        return new TaskResponse(
                task.getId(), task.getTitle(), task.getCustomerName(), task.getCustomerId(), task.getCustomerStatus(), task.getOwner(),
                task.getDueAt(), task.getTaskType(), task.getStatus(), task.isCompleted(), task.getPriority(),
                task.getFollowedAt(), task.getCreatedAt(), task.getUpdatedAt());
    }
}
