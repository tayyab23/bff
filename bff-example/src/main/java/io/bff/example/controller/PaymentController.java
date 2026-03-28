package io.bff.example.controller;

import io.bff.annotation.BffIngredient;
import io.bff.example.generated.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class PaymentController {

    @BffIngredient(recipe = "payments", name = "getPaymentMethods",
                   forwardHeaders = {"Authorization"})
    @GetMapping("/api/payment-methods")
    public ResponseEntity<PaymentMethodList> getPaymentMethods(
            @RequestParam String customerId,
            @RequestHeader("Authorization") String authorization) {

        List<PaymentMethod> methods = List.of(
            new PaymentMethod().id("pm-visa-4242").type(PaymentMethod.TypeEnum.CARD)
                .last4("4242").expiryMonth(12).expiryYear(2027).isDefault(true),
            new PaymentMethod().id("pm-bank-9999").type(PaymentMethod.TypeEnum.BANK_ACCOUNT)
                .last4("9999").expiryMonth(null).expiryYear(null).isDefault(false)
        );

        return ResponseEntity.ok(new PaymentMethodList().methods(methods));
    }

    @BffIngredient(recipe = "payments", name = "submitPayment",
                   forwardHeaders = {"Authorization"},
                   customHeaders = {"X-Idempotency-Key"})
    @PostMapping("/api/payments")
    public ResponseEntity<PaymentConfirmation> submitPayment(
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "X-Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody SubmitPaymentRequest request) {

        return ResponseEntity.ok(new PaymentConfirmation()
            .paymentId("pay-" + System.currentTimeMillis())
            .status(PaymentConfirmation.StatusEnum.CONFIRMED)
            .amount(request.getAmount())
            .currency(request.getCurrency())
            .processedAt(Instant.now().toString())
            .receiptUrl("https://receipts.example.com/pay-" + System.currentTimeMillis()));
    }

    @BffIngredient(recipe = {"payments", "dashboard"}, name = "sendNotification",
                   forwardHeaders = {"Authorization"})
    @PostMapping("/api/notifications/send")
    public ResponseEntity<NotificationResult> sendNotification(
            @RequestHeader("Authorization") String authorization,
            @RequestBody SendNotificationRequest request) {

        return ResponseEntity.ok(new NotificationResult()
            .notificationId("notif-" + System.currentTimeMillis())
            .status("SENT")
            .sentAt(Instant.now().toString()));
    }
}
