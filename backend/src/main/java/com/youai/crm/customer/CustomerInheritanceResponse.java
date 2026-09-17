package com.youai.crm.customer;

public record CustomerInheritanceResponse(
        String sourceOwner,
        String targetOwner,
        int transferredCount) {
}
