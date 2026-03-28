package io.bff.example.model;

public record Account(
    String accountId,
    String email,
    String fullName,
    String billingGroupId,
    String customerId,
    String plan,
    String region,
    String createdAt
) {}
