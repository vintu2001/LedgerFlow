package com.ledgerflow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ledgerflow.model.EventCheckpoint;
import com.ledgerflow.model.LedgerEntry;
import com.ledgerflow.model.TransactionEvent;
import com.ledgerflow.repository.BalanceRepository;
import com.ledgerflow.repository.CheckpointRepository;
import com.ledgerflow.repository.LedgerEntryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LedgerProcessor {

    private static final Logger log = LoggerFactory.getLogger(LedgerProcessor.class);

    private final LedgerEntryRepository ledgerRepo;
    private final BalanceRepository balanceRepo;
    private final CheckpointRepository checkpointRepo;
    private final ObjectMapper objectMapper;
    private final String consumerGroup;

    public LedgerProcessor(LedgerEntryRepository ledgerRepo,
                           BalanceRepository balanceRepo,
                           CheckpointRepository checkpointRepo,
                           ObjectMapper objectMapper,
                           @Value("${azure.eventhub.consumer-group:$Default}") String consumerGroup) {
        this.ledgerRepo = ledgerRepo;
        this.balanceRepo = balanceRepo;
        this.checkpointRepo = checkpointRepo;
        this.objectMapper = objectMapper;
        this.consumerGroup = consumerGroup;
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void process(String payload, String partitionId, long sequenceNumber) {
        TransactionEvent event = deserialize(payload);

        if (ledgerRepo.existsByTransactionId(event.transactionId())) {
            log.debug("Duplicate transaction {}, skipping", event.transactionId());
            saveCheckpoint(partitionId, sequenceNumber);
            return;
        }

        LedgerEntry entry = LedgerEntry.builder()
            .transactionId(event.transactionId())
            .accountId(event.accountId())
            .entryType(event.entryType())
            .amountCents(event.amountCents())
            .currency(event.currency())
            .metadata(event.metadata())
            .partitionId(partitionId)
            .sequenceNumber(sequenceNumber)
            .build();
        LedgerEntry saved = ledgerRepo.save(entry);

        long delta = computeDelta(event);
        balanceRepo.updateBalance(
            event.accountId(), delta, event.entryType(),
            saved.getEntryId(), event.transactionId()
        );

        saveCheckpoint(partitionId, sequenceNumber);

        log.info("Processed transaction {} for account {}: {} {} cents",
            event.transactionId(), event.accountId(),
            event.entryType(), event.amountCents());
    }

    private long computeDelta(TransactionEvent event) {
        return switch (event.entryType()) {
            case "charge"     -> -event.amountCents();
            case "credit"     -> event.amountCents();
            case "refund"     -> event.amountCents();
            case "adjustment" -> event.amountCents();
            default -> throw new IllegalArgumentException("Unknown entry type: " + event.entryType());
        };
    }

    private void saveCheckpoint(String partitionId, long sequenceNumber) {
        EventCheckpoint checkpoint = checkpointRepo
            .findByConsumerGroupAndPartitionId(consumerGroup, partitionId)
            .orElseGet(() -> EventCheckpoint.builder()
                .consumerGroup(consumerGroup)
                .partitionId(partitionId)
                .build());
        checkpoint.setSequenceNumber(sequenceNumber);
        checkpointRepo.save(checkpoint);
    }

    private TransactionEvent deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, TransactionEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to deserialize transaction event", e);
        }
    }
}
