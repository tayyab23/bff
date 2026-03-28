package io.bff.example.controller;

import io.bff.annotation.BffIngredient;
import io.bff.example.generated.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class InvoiceController {

    @BffIngredient(recipe = {"payments", "dashboard"}, name = "getInvoices",
                   forwardHeaders = {"Authorization"})
    @GetMapping("/api/invoices")
    public ResponseEntity<InvoiceList> getInvoices(
            @RequestParam String billingGroupId,
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "20") int limit,
            @RequestHeader("Authorization") String authorization) {

        List<Invoice> items = List.of(
            new Invoice().id("inv-001").amount(249.99).status(Invoice.StatusEnum.UNPAID)
                .dueDate("2026-04-01").description("Pro Plan - March 2026"),
            new Invoice().id("inv-002").amount(249.99).status(Invoice.StatusEnum.OVERDUE)
                .dueDate("2026-03-01").description("Pro Plan - February 2026")
        );

        return ResponseEntity.ok(new InvoiceList()
            .items(items)
            .total(499.98)
            .currency("USD")
            .page(1));
    }
}
