package io.bff.example.model;

public record PaymentConfirmation(String paymentId, String status, double amount) {}
