package io.bff.example.model;

import java.util.List;

public record PaymentMethodList(List<PaymentMethod> methods) {
    public record PaymentMethod(String id, String type, String last4) {}
}
