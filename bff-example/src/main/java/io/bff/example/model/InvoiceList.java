package io.bff.example.model;

import java.util.List;

public record InvoiceList(List<Invoice> items, double total) {
    public record Invoice(String id, double amount, String status) {}
}
