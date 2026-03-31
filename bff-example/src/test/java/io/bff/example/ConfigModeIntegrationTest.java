package io.bff.example;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = "spring.profiles.active=configmode")
@AutoConfigureMockMvc
class ConfigModeIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;

    private static final String AUTH = "Bearer demo-token";

    @Test
    void configMode_singleIngredient() throws Exception {
        var body = Map.of("ingredients", List.of(
            Map.of("id", "getAccount", "params", Map.of("accountId", "acc-123"))
        ));
        mvc.perform(post("/bff/reporting")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200))
            .andExpect(jsonPath("$.results.getAccount.body.accountId").value("acc-123"));
    }

    @Test
    void configMode_reportSummaryWithDependency() throws Exception {
        var body = Map.of("ingredients", List.of(
            Map.of("id", "getAccount", "params", Map.of("accountId", "acc-123")),
            Map.of("id", "getReportSummary",
                "map", Map.of("path", Map.of("accountId", "getAccount::body::${accountId}")))
        ));
        mvc.perform(post("/bff/reporting")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getReportSummary.status").value(200))
            .andExpect(jsonPath("$.results.getReportSummary.body.totalSpend").value(14250.00))
            .andExpect(jsonPath("$.results.getReportSummary.body.accountId").value("acc-123"));
    }

    @Test
    void configMode_fullReportingRecipe() throws Exception {
        var body = Map.of("ingredients", List.of(
            Map.of("id", "getAccount", "params", Map.of("accountId", "acc-123")),
            Map.of("id", "getReportSummary",
                "map", Map.of("path", Map.of("accountId", "getAccount::body::${accountId}"))),
            Map.of("id", "getSpendingTrends",
                "map", Map.of("query", Map.of("accountId", "getAccount::body::${accountId}")))
        ));
        mvc.perform(post("/bff/reporting")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.results.getAccount.status").value(200))
            .andExpect(jsonPath("$.results.getReportSummary.status").value(200))
            .andExpect(jsonPath("$.results.getSpendingTrends.status").value(200))
            .andExpect(jsonPath("$.results.getSpendingTrends.body.months").isArray());
    }

    @Test
    void configMode_unknownIngredient_returns400() throws Exception {
        var body = Map.of("ingredients", List.of(
            Map.of("id", "nonExistent", "params", Map.of("accountId", "acc-123"))
        ));
        mvc.perform(post("/bff/reporting")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void configMode_annotatedRecipe_notAvailable() throws Exception {
        var body = Map.of("ingredients", List.of(
            Map.of("id", "getAccount", "params", Map.of("accountId", "acc-123"))
        ));
        // "payments" recipe was defined via annotations, not config — should not exist in config mode
        mvc.perform(post("/bff/payments")
                .header("Authorization", AUTH)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(body)))
            .andExpect(status().isBadRequest());
    }
}
