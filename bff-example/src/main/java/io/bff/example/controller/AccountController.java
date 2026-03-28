package io.bff.example.controller;

import io.bff.annotation.BffIngredient;
import io.bff.example.generated.model.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
public class AccountController {

    @BffIngredient(recipe = {"payments", "dashboard"}, name = "getAccount",
                   forwardHeaders = {"Authorization"})
    @GetMapping("/api/accounts/{accountId}")
    public ResponseEntity<Account> getAccount(
            @PathVariable String accountId,
            @RequestHeader("Authorization") String authorization) {

        Account account = new Account()
            .accountId(accountId)
            .email("jane.doe@example.com")
            .fullName("Jane Doe")
            .billingGroupId("bg-" + accountId)
            .customerId("cust-" + accountId)
            .plan(Account.PlanEnum.PRO)
            .region("us-east-1")
            .createdAt("2023-06-15T10:00:00Z");

        return ResponseEntity.ok(account);
    }

    @BffIngredient(recipe = "dashboard", name = "getUserProfile",
                   forwardHeaders = {"Authorization", "Accept-Language"})
    @GetMapping("/api/users/{userId}/profile")
    public ResponseEntity<UserProfile> getUserProfile(
            @PathVariable String userId,
            @RequestHeader("Authorization") String authorization,
            @RequestHeader(value = "Accept-Language", defaultValue = "en-US") String acceptLanguage) {

        UserProfile profile = new UserProfile()
            .userId(userId)
            .displayName("Jane Doe")
            .avatarUrl("https://cdn.example.com/avatars/" + userId + ".png")
            .timezone("America/New_York")
            .locale(acceptLanguage)
            .notificationsEnabled(true);

        return ResponseEntity.ok(profile);
    }
}
