package com.ledgerflow.service;

import com.ledgerflow.api.dto.TransactionRequest;
import com.ledgerflow.api.dto.TransactionResponse;
import com.ledgerflow.event.EventPublisher;
import com.ledgerflow.model.TransactionEvent;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock private EventPublisher eventPublisher;
    @InjectMocks private TransactionService transactionService;

    @Test
    void submitChargeTransaction() {
        TransactionRequest request = new TransactionRequest(
            "acct-123", "charge", 1500, "USD", null);

        TransactionResponse response = transactionService.submitTransaction(request);

        assertNotNull(response.transactionId());
        assertEquals("pending", response.status());
        assertNotNull(response.timestamp());

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(eventPublisher).publish(captor.capture());

        TransactionEvent event = captor.getValue();
        assertEquals("acct-123", event.accountId());
        assertEquals("charge", event.entryType());
        assertEquals(1500, event.amountCents());
        assertEquals("USD", event.currency());
    }

    @Test
    void submitTransactionWithDefaultCurrency() {
        TransactionRequest request = new TransactionRequest(
            "acct-456", "credit", 2000, null, null);

        TransactionResponse response = transactionService.submitTransaction(request);

        assertNotNull(response);

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(eventPublisher).publish(captor.capture());
        assertEquals("USD", captor.getValue().currency());
    }

    @Test
    void submitRefundTransaction() {
        TransactionRequest request = new TransactionRequest(
            "acct-789", "refund", 500, "USD", "{\"reason\": \"overcharge\"}");

        TransactionResponse response = transactionService.submitTransaction(request);

        assertNotNull(response.transactionId());
        assertEquals("pending", response.status());

        ArgumentCaptor<TransactionEvent> captor = ArgumentCaptor.forClass(TransactionEvent.class);
        verify(eventPublisher).publish(captor.capture());

        TransactionEvent event = captor.getValue();
        assertEquals("acct-789", event.accountId());
        assertEquals("refund", event.entryType());
        assertEquals(500, event.amountCents());
        assertEquals("{\"reason\": \"overcharge\"}", event.metadata());
    }
}
