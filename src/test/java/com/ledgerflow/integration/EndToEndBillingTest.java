package com.ledgerflow.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.api.dto.TransactionRequest;
import com.ledgerflow.event.EventPublisher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class EndToEndBillingTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockBean private EventPublisher eventPublisher;

    @Test
    void submitTransactionReturnsAccepted() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "acct-test-001", "charge", 2500, "USD", null);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.transactionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void submitCreditTransactionReturnsAccepted() throws Exception {
        TransactionRequest request = new TransactionRequest(
            "acct-test-002", "credit", 1000, "USD", null);

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.transactionId").isNotEmpty())
            .andExpect(jsonPath("$.status").value("pending"));
    }

    @Test
    void submitInvalidTransactionReturnsBadRequest() throws Exception {
        String invalidPayload = """
            {"accountId": "", "entryType": "charge", "amountCents": 100}
            """;

        mockMvc.perform(post("/api/transactions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidPayload))
            .andExpect(status().isBadRequest());
    }

    @Test
    void getBalanceReturnsDefaultForNewAccount() throws Exception {
        mockMvc.perform(get("/api/accounts/acct-nonexistent/balance"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accountId").value("acct-nonexistent"))
            .andExpect(jsonPath("$.balanceCents").value(0));
    }
}
