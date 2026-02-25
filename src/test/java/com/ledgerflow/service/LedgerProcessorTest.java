package com.ledgerflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ledgerflow.model.EventCheckpoint;
import com.ledgerflow.model.LedgerEntry;
import com.ledgerflow.model.TransactionEvent;
import com.ledgerflow.repository.BalanceRepository;
import com.ledgerflow.repository.CheckpointRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LedgerProcessorTest {

    @Mock private LedgerEntryRepository ledgerRepo;
    @Mock private BalanceRepository balanceRepo;
    @Mock private CheckpointRepository checkpointRepo;

    private LedgerProcessor processor;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        processor = new LedgerProcessor(
            ledgerRepo, balanceRepo, checkpointRepo, objectMapper, "test-group");
    }

    @Test
    void processChargeTransaction() throws Exception {
        TransactionEvent event = new TransactionEvent(
            "txn-001", "acct-123", "charge", 1500, "USD", null, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        when(ledgerRepo.existsByTransactionId("txn-001")).thenReturn(false);
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> {
            LedgerEntry entry = invocation.getArgument(0);
            entry.setEntryId(1L);
            return entry;
        });
        when(checkpointRepo.findByConsumerGroupAndPartitionId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        processor.process(payload, "0", 100L);

        verify(ledgerRepo).save(any(LedgerEntry.class));
        verify(balanceRepo).updateBalance(eq("acct-123"), eq(-1500L), eq("charge"), eq(1L), eq("txn-001"));
        verify(checkpointRepo).save(any(EventCheckpoint.class));
    }

    @Test
    void skipDuplicateTransaction() throws Exception {
        TransactionEvent event = new TransactionEvent(
            "txn-dup", "acct-123", "credit", 500, "USD", null, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        when(ledgerRepo.existsByTransactionId("txn-dup")).thenReturn(true);
        when(checkpointRepo.findByConsumerGroupAndPartitionId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        processor.process(payload, "0", 200L);

        verify(ledgerRepo, never()).save(any());
        verify(balanceRepo, never()).updateBalance(any(), anyLong(), any(), anyLong(), any());
        verify(checkpointRepo).save(any(EventCheckpoint.class));
    }

    @Test
    void processCreditTransaction() throws Exception {
        TransactionEvent event = new TransactionEvent(
            "txn-002", "acct-456", "credit", 2000, "USD", null, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        when(ledgerRepo.existsByTransactionId("txn-002")).thenReturn(false);
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> {
            LedgerEntry entry = invocation.getArgument(0);
            entry.setEntryId(2L);
            return entry;
        });
        when(checkpointRepo.findByConsumerGroupAndPartitionId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        processor.process(payload, "1", 50L);

        verify(balanceRepo).updateBalance(eq("acct-456"), eq(2000L), eq("credit"), eq(2L), eq("txn-002"));
    }

    @Test
    void processRefundTransaction() throws Exception {
        TransactionEvent event = new TransactionEvent(
            "txn-003", "acct-789", "refund", 750, "USD", null, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        when(ledgerRepo.existsByTransactionId("txn-003")).thenReturn(false);
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> {
            LedgerEntry entry = invocation.getArgument(0);
            entry.setEntryId(3L);
            return entry;
        });
        when(checkpointRepo.findByConsumerGroupAndPartitionId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        processor.process(payload, "2", 75L);

        verify(balanceRepo).updateBalance(eq("acct-789"), eq(750L), eq("refund"), eq(3L), eq("txn-003"));
    }

    @Test
    void processAdjustmentTransaction() throws Exception {
        TransactionEvent event = new TransactionEvent(
            "txn-004", "acct-321", "adjustment", 300, "USD", null, Instant.now());
        String payload = objectMapper.writeValueAsString(event);

        when(ledgerRepo.existsByTransactionId("txn-004")).thenReturn(false);
        when(ledgerRepo.save(any(LedgerEntry.class))).thenAnswer(invocation -> {
            LedgerEntry entry = invocation.getArgument(0);
            entry.setEntryId(4L);
            return entry;
        });
        when(checkpointRepo.findByConsumerGroupAndPartitionId(anyString(), anyString()))
            .thenReturn(Optional.empty());

        processor.process(payload, "0", 300L);

        verify(balanceRepo).updateBalance(eq("acct-321"), eq(300L), eq("adjustment"), eq(4L), eq("txn-004"));
    }
}
