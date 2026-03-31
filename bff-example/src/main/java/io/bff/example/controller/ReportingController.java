package io.bff.example.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Simulates a third-party or cross-team controller with no @BffIngredient annotations.
 * These endpoints are aggregated via config mode in application.yml.
 */
@RestController
public class ReportingController {

    @GetMapping("/api/reports/summary/{accountId}")
    public ResponseEntity<Map<String, Object>> getReportSummary(@PathVariable String accountId) {
        return ResponseEntity.ok(Map.of(
            "accountId", accountId,
            "totalSpend", 14250.00,
            "invoiceCount", 7,
            "lastPaymentDate", "2026-03-15"
        ));
    }

    @GetMapping("/api/reports/trends")
    public ResponseEntity<Map<String, Object>> getSpendingTrends(@RequestParam String accountId) {
        return ResponseEntity.ok(Map.of(
            "accountId", accountId,
            "months", List.of(
                Map.of("month", "2026-01", "spend", 4200.00),
                Map.of("month", "2026-02", "spend", 3800.00),
                Map.of("month", "2026-03", "spend", 6250.00)
            )
        ));
    }
}
