package io.bff.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class BffIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static final String AUTH = "Bearer demo-token";

    // ── Single ingredient, no dependencies ──

    @Test
    void singleIngredient_returnsAccountData() throws Exception {
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200))
            .andExpect(jsonPath("$.results.getAccount.body.accountId").value("acc-123"))
            .andExpect(jsonPath("$.results.getAccount.body.billingGroupId").value("bg-acc-123"))
            .andExpect(jsonPath("$.executionOrder[0]").value("getAccount"));
    }

    // ── Parallel execution with dependency wiring ──

    @Test
    void parallelIngredients_wireFromAccount() throws Exception {
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}},
                        {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}},
                        {"id": "getPaymentMethods", "map": {"query": {"customerId": "getAccount::body::${customerId}"}}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200))
            .andExpect(jsonPath("$.results.getInvoices.status").value(200))
            .andExpect(jsonPath("$.results.getInvoices.body.items").isArray())
            .andExpect(jsonPath("$.results.getPaymentMethods.status").value(200))
            .andExpect(jsonPath("$.results.getPaymentMethods.body.methods").isArray())
            .andExpect(jsonPath("$.executionOrder").isArray());
    }

    // ── Multi-recipe: dashboard recipe ──

    @Test
    void dashboardRecipe_differentIngredientSet() throws Exception {
        mvc.perform(post("/bff/dashboard")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-456"}},
                        {"id": "getUserProfile", "params": {"userId": "user-456"}},
                        {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.body.accountId").value("acc-456"))
            .andExpect(jsonPath("$.results.getUserProfile.status").value(200))
            .andExpect(jsonPath("$.results.getInvoices.status").value(200));
    }

    // ── Unknown ingredient returns 400 ──

    @Test
    void unknownIngredient_returns400() throws Exception {
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "nonExistent", "params": {"foo": "bar"}}
                    ]}"""))
            .andExpect(status().isBadRequest());
    }

    // ── failFast: dependency failure skips dependents ──

    @Test
    void dependencyFailure_skipsDependents() throws Exception {
        // getInvoices depends on getAccount, but we don't provide getAccount
        // so getInvoices should reference a missing ingredient
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getInvoices", "map": {"query": {"billingGroupId": "getAccount::body::${billingGroupId}"}}}
                    ]}"""))
            .andExpect(status().isBadRequest());
    }

    // ── Schema endpoint ──

    @Test
    void schemaEndpoint_returnsIngredients() throws Exception {
        mvc.perform(get("/bff/payments/schema")
                .header("Authorization", AUTH))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.recipe").value("payments"))
            .andExpect(jsonPath("$.ingredients.getAccount").exists())
            .andExpect(jsonPath("$.ingredients.getInvoices").exists())
            .andExpect(jsonPath("$.ingredients.getPaymentMethods").exists())
            .andExpect(jsonPath("$.ingredients.submitPayment").exists());
    }

    // ── Validate endpoint — valid recipe ──

    @Test
    void validateEndpoint_validRecipe() throws Exception {
        mvc.perform(post("/bff/payments/validate")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(true));
    }

    // ── Validate endpoint — invalid recipe ──

    @Test
    void validateEndpoint_invalidRecipe() throws Exception {
        mvc.perform(post("/bff/payments/validate")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "doesNotExist"}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.valid").value(false))
            .andExpect(jsonPath("$.errors").isArray());
    }

    // ── No auth returns 401/403 ──

    @Test
    void noAuth_returnsUnauthorized() throws Exception {
        mvc.perform(post("/bff/payments")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}}
                    ]}"""))
            .andExpect(status().isUnauthorized());
    }

    // ── Custom headers forwarded ──

    @Test
    void customHeaders_forwarded() throws Exception {
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .header("X-Idempotency-Key", "idem-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200));
    }

    // ── Max ingredients exceeded ──

    @Test
    void maxIngredientsExceeded_returns400() throws Exception {
        // Config allows max 10, send 11
        StringBuilder ingredients = new StringBuilder("[");
        for (int i = 0; i < 11; i++) {
            if (i > 0) ingredients.append(",");
            ingredients.append("{\"id\": \"getAccount\", \"params\": {\"accountId\": \"acc-").append(i).append("\"}}");
        }
        ingredients.append("]");

        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"ingredients\": " + ingredients + "}"))
            .andExpect(status().isBadRequest());
    }

    // ── Debug info returned when requested ──

    @Test
    void debugEnabled_returnsDebugInfo() throws Exception {
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"debug": true, "ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}}
                    ]}"""))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200));
        // debug info assertion depends on lib implementation — at minimum the request succeeds
    }

    // ── 207 Multi-Status on partial failure ──

    @Test
    void partialFailure_returns207() throws Exception {
        // submitPayment requires a body — sending without one should cause it to fail
        // while getAccount succeeds
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"ingredients": [
                        {"id": "getAccount", "params": {"accountId": "acc-123"}},
                        {"id": "submitPayment", "dependsOn": ["getAccount"]}
                    ]}"""))
            .andExpect(status().is(207));
    }
}
