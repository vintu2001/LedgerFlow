package com.ledgerflow.service;

import com.ledgerflow.api.dto.TransactionRequest;
import com.ledgerflow.api.dto.TransactionResponse;
import com.ledgerflow.event.EventPublisher;
import com.ledgerflow.model.TransactionEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);

    private final EventPublisher eventPublisher;

    public TransactionService(EventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    public TransactionResponse submitTransaction(TransactionRequest request) {
        String transactionId = UUID.randomUUID().toString();

        TransactionEvent event = new TransactionEvent(
            transactionId,
            request.accountId(),
            request.entryType(),
            request.amountCents(),
            request.currency() != null ? request.currency() : "USD",
            request.metadata(),
            Instant.now()
        );

        eventPublisher.publish(event);

        log.info("Submitted transaction {} for account {}", transactionId, request.accountId());

        return new TransactionResponse(transactionId, "pending", Instant.now());
    }
}
