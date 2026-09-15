package com.youai.crm.customer;

import java.time.LocalDateTime;

public record FollowUpAnnotationResponse(
        String recordId,
        String recordType,
        boolean favorite,
        String comment,
        String author,
        LocalDateTime updatedAt) {

    public static FollowUpAnnotationResponse from(FollowUpAnnotation annotation) {
        return new FollowUpAnnotationResponse(annotation.getRecordId(), annotation.getRecordType(),
                annotation.isFavorite(), annotation.getComment(), annotation.getAuthor(), annotation.getUpdatedAt());
    }
}
